/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define(function () {
	function validateMail() {
		const mail = _('mail').val();
		if (mail.match(/^.*@(gmail|yahoo|free|sfr|live|hotmail)\.[a-zA-Z]+$/i)) {
			validationManager.addWarn(_('mail'), 'warn-mail-perso');
		} else {
			validationManager.reset(_('mail'));
		}
	}

	const current = {

		/**
		 * Flag objects
		 */
		table: null,
		search: false,
		suspendSearch: false,

		/**
		 * Edited users' identifier
		 */
		currentId: 0,

		initialize: function (parameters) {
			current.onHashChange(parameters);
		},

		/**
		 * Manage internal navigation from URL.
		 * @param Accept group and company filtering from parameters.
		 */
		onHashChange: function (parameters) {
			// Search mode
			current.currentId = null;
			current.initializeSearch();
			if (parameters) {
				current.suspendSearch = true;
				parameters.split('/').forEach(p => {
					let kv = p.split('=');
					// Group/company filtering
					kv.length === 2 && _('search-' + kv[0]).select2('data', kv[1]).closest('.form-group').removeClass('is-empty');
				});
				current.suspendSearch = false;
			}

			// Also initialize the datatables component
			current.initializeDataTable();
			$(function () {
				_('search').trigger('focus');
			});
		},

		/**
		 * Initialize the search UI components
		 */
		initializeSearch: function () {
			if (current.search) {
				_('search-company').select2('data', null);
				_('search-group').select2('data', null);
				return;
			}
			current.search = true;

			// User edition pop-up
			_('popup').on('shown.bs.modal', function () {
				current.currentId ? _('groups').select2('focus') : _('id').focus();
			}).on('submit', function (e) {
				e.preventDefault();
				current.save();
				return false;
			}).on('show.bs.modal', function (event) {
				const $source = $(event.relatedTarget);
				const $tr = $source.closest('tr');
				let uc = ($tr.length && current.table.fnGetData($tr[0])) || {};

				// 'Create another user' option, is only available for creation mode
				_('create-another').removeAttr('checked').closest('label')[uc.id ? 'addClass' : 'removeClass']('hide');
				current.fillPopup(uc);
			});
			_('company').select2({
				minimumInputLength: 0,
				initSelection: function (element, callback) {
					callback({
						id: element.val(),
						text: element.val()
					});
				},
				formatSearching: function () {
					return current.$messages.loading;
				},
				ajax: {
					url: REST_PATH + 'service/id/company/filter/write',
					dataType: 'json',
					data: function (term, page) {
						return {
							q: term, // search term
							rows: 15,
							page: page,
							filters: '{}',
							sidx: 'name',
							sord: 'asc'
						};
					},
					results: function (data, page) {
						return {
							more: data.recordsFiltered > page * 10,
							results: data.data.map(d => ({ id: d, text: d }))
						};
					}
				}
			});
			_('groups').select2({
				multiple: true,
				createSearchChoice: function () {
					// Disable additional values
					return null;
				},
				formatSearching: function () {
					return current.$messages.loading;
				},
				ajax: {
					url: REST_PATH + 'service/id/group/filter/write',
					dataType: 'json',
					data: function (term, page) {
						return {
							q: term, // search term
							rows: 15,
							page: page,
							filters: '{}',
							sidx: 'name',
							sord: 'asc'
						};
					},
					results: function (data, page) {
						return {
							more: data.recordsFiltered > page * 10,
							results: data.data.map(d => ({ id: d, text: d }))
						};
					}
				}
			});
			current.$main.newSelect2Group('#search-group');
			current.$main.newSelect2Company('#search-company');
			$('#search-group,#search-company').on('change', function () {
				current.refreshDataTable();
			});
			_('mail').on('blur', validateMail);
			_('importPopup')
				.on('shown.bs.modal', () => $('.import-options input:checked').trigger('focus'))
				.on('show.bs.modal', function () {
					$('.import-progress').attr('aria-valuenow', '0').css('width', '0%').removeClass('progress-bar progress-bar-striped progress-bar-striped').empty();
					$('.import-summary').addClass('hide').empty();
					_('quiet').prop('checked', false);
					current.$parent.unscheduleUploadStep();
				}).on('submit', () => current.saveBatch());
			$('.import-options input').on('change', function () {
				$('.import-options-details').addClass('hidden').filter($(this).hasClass('import-options-full') ? '.import-options-full' : '.import-options-atomic').removeClass('hidden');
			});

			// Data tables filters
			_('create').on('click', function () {
				current.$parent.requireAgreement(current.showPopup, $(this));
			});
			_('upload-new').on('click', function () {
				current.$parent.requireAgreement(current.showPopupImport, $(this));
			});
			// Global datatables filter
			_('search').on('keyup', function () {
				current.table && current.table.fnFilter($(this).val());
			});

			_('table').on('click', '.reset', current.resetUserPassword).on('click', '.delete', current.deleteUser).on('click', '.lock', current.lockUser).on('click', '.unlock', current.unlockUser).on('click', '.isolate', current.isolateUser).on('click', '.restore', current.restoreUser).on('click', '.update', function () {
				current.$parent.requireAgreement(current.showPopup, $(this));
			});

			_('rest-password-popup').on('hide.bs.modal', function () {
				_('generated-password').val('');
			}).on('show.bs.modal', function () {
				_('show-password').removeAttr('checked');
			});

			_('show-password').on('change', function () {
				if ($(this).is(':checked')) {
					_('generated-password')[0].type = "text";
				} else {
					_('generated-password')[0].type = "password";
				}
			});

			// Also initialize the datatables component
			current.initializeDataTable();
		},

		/**
		 * refresh datatable with filters
		 */
		refreshDataTable: function () {
			if (current.table && !current.suspendSearch) {
				current.table.api().ajax.reload();
			}
		},

		/**
		 * Initialize the users datatables (server AJAX)
		 */
		initializeDataTable: function () {
			if (current.table) {
				current.refreshDataTable();
			} else {
				current.table = _('table').dataTable({
					dom: 'rt<"row"<"col-xs-6"i><"col-xs-6"p>>',
					serverSide: true,
					searching: true,
					ajax: function () {
						const company = $('#search-company').select2('data');
						const group = $('#search-group').select2('data');
						if (company || group) {
							return REST_PATH + 'service/id/user?' + (company ? 'company=' + (company.id || company) : '') + ((company && group) ? '&' : '') + (group ? 'group=' + (group.id || group) : '');
						}
						return REST_PATH + 'service/id/user';
					},
					columns: [
						{
							data: 'id',
							width: '120px',
							render: function (_i, _j, data) {
								return current.$main.getUserLoginLink(data);
							}
						}, {
							data: 'firstName',
							className: 'truncate'
						}, {
							data: 'lastName',
							className: 'truncate'
						}, {
							data: 'company',
							className: 'hidden-xs truncate'
						}, {
							data: 'mails',
							className: 'hidden-md hidden-sm hidden-xs truncate',
							render: function (mails) {
								return (mails && mails.length) ? '<a href="mailto:' + mails[0] + '">' + mails[0] + '</a>' : '';
							}
						}, {
							data: 'groups',
							orderable: false,
							className: 'hidden-sm hidden-xs truncate',
							render: function (_i, _j, data) {
								return data.groups.map(d => d.name);
							}
						}, {
							data: null,
							width: '36px',
							orderable: false,
							render: function (_i, _j, data) {
								let editLink = '<a class="update"><i class="fas fa-pencil-alt" data-toggle="tooltip" title="' + current.$messages.update + '"></i></a>';
								if (data.canWrite) {
									editLink += '<div class="btn-group"><i data-toggle="dropdown" class="fas fa-cog"></i><ul class="dropdown-menu dropdown-menu-right">';
									if (data.isolated) {
										// Isolated -> restore
										editLink += '<li><a class="restore"><i class="menu-icon fa fa-sign-in"></i> ' + current.$messages.restore + '</a></li>';
									} else if (data.locked) {
										// Locked -> unlock or isolate
										editLink += '<li><a class="unlock"><i class="menu-icon fa fa-unlock"></i> ' + current.$messages.unlock + '</a></li>';
										editLink += '<li><a class="isolate"><i class="menu-icon fa fa-sign-out"></i> ' + current.$messages.isolate + '</a></li>';
									} else {
										// Unlocked -> lock or isolate
										editLink += '<li><a class="lock"><i class="menu-icon fa fa-lock"></i> ' + current.$messages.lock + '</a></li>';
										editLink += '<li><a class="isolate"><i class="menu-icon fa fa-sign-out"></i> ' + current.$messages.isolate + '</a></li>';
									}

									// Delete icon
									editLink += '<li><a class="delete"><i class="menu-icon fa fa-trash-alt"></i> ' + current.$messages.delete + '</a></li>';
									editLink += '<li><a class="reset"><i class="menu-icon fas fa-sync-alt"></i> ' + current.$messages.reset + '</a></li>';
									editLink += '</ul>';
									editLink += '</div>';
								}

								return editLink;
							}
						}
					]
				});
			}
		},

		showPopup: function ($context) {
			_('popup').modal('show', $context);
		},
		showPopupImport: function ($context) {
			_('importPopup').modal('show', $context);
		},

		formToObject: function () {
			return {
				id: ($('#id').val() || '').toLowerCase(),
				firstName: $('#firstName').val() || null,
				lastName: $('#lastName').val() || null,
				department: $('#department').val() || null,
				localId: $('#localId').val() || null,
				mail: $('#mail').val() || null,
				company: $('#company').val().toLowerCase(),
				groups: $('#groups').val() ? $('#groups').val().toLowerCase().split(',') : []
			};
		},

		saveBatch: function () {
			const $popup = _('importPopup');
			const mode = $popup.find('.import-options input:checked').is('.import-options-full') ? 'full' : 'atomic';
			_('quiet').val(_('quiet').is(':checked') ? 'true' : 'false');
			$popup.ajaxSubmit({
				url: REST_PATH + 'service/id/user/batch/' + mode,
				type: 'POST',
				dataType: 'json',
				beforeSubmit: function () {
					// Reset the summary
					$('.import-summary').html('Uploading...').removeClass('alert-danger alert-success hide').addClass('alert-info');
					$('.import-progress').addClass('progress-bar progress-bar-striped progress-bar-striped');
					validationManager.reset(_('importPopup'));
					validationManager.mapping.DEFAULT = 'csv-file';
				},
				success: function (id) {
					$('.import-summary').html('Processing...');
					current.$parent.scheduleUploadStep('service/id/user/batch/' + mode, id, () => current.table?.api().ajax.reload());
				}
			});
			e.preventDefault();
			return false;
		},

		save: function () {
			// Might be a long operation, add a pending indicator
			_('confirmCreate').button('loading');
			const data = current.formToObject();
			$.ajax({
				type: current.currentId ? 'PUT' : 'POST',
				url: REST_PATH + 'service/id/user',
				dataType: 'json',
				contentType: 'application/json',
				data: JSON.stringify(data),
				success: function () {
					if (current.currentId) {
						notifyManager.notify(Handlebars.compile(current.$messages.updated)(data.id));
					} else {
						notifyManager.notify(Handlebars.compile(current.$messages['created-account'])(data));
					}
					current.table && current.table.api().ajax.reload();
					if ($('#create-another:checked').length) {
						// Only reset the popup
						current.fillPopup({});
						_('id').trigger('focus');
					} else {
						_('popup').modal('hide');
					}
				},
				complete: function () {
					// Whatever the result, stop the indicator
					_('confirmCreate').button('complete');
				}
			});
		},

		/**
		 * Fill the popup with given entity.
		 * @param {Object} uc, the entity corresponding to the user.
		 */
		fillPopup: function (uc) {
			validationManager.reset(_('popup'));
			current.currentId = uc.id;
			_('id').val(uc.id || '');
			_('firstName').val(uc.firstName || '');
			_('lastName').val(uc.lastName || '');
			_('department').val(uc.department || '');
			_('localId').val(uc.localId || '');
			_('mail').val((uc.mails && uc.mails[0]) || '');
			_('company').select2('val', uc.company || null);
			_('groups').select2('data', (uc.groups || []).map(g => ({
				id: g.name,
				text: g.name,
				locked: !g.canWrite
			})));

			// id and company are read-only
			if (uc.id) {
				_('id').attr('readonly', 'readonly');
			} else {
				_('id').removeAttr('readonly');
			}

			// Mark as read-only the fields the user cannot update
			const $inputs = _('popup').find('input[type="text"]').not('#groups').not('.select2-input,.select2-focusser');
			if (uc.canWrite || !uc.id) {
				$inputs.removeAttr('readonly');
				if (uc.isolated) {
					_('company').attr('readonly', 'readonly');
				}
			} else {
				$inputs.attr('readonly', 'readonly');
			}
		},

		/**
		 * Delete the selected user after popup confirmation, or directly from its identifier.
		 */
		deleteUser: function (id, name) {
			if (typeof id === 'string') {
				// Delete without confirmation
				$.ajax({
					type: 'DELETE',
					url: REST_PATH + 'service/id/user/' + id,
					success: function () {
						notifyManager.notify(Handlebars.compile(current.$messages.deleted)(name));
						current.table && current.table.api().ajax.reload();
					}
				});
			} else {
				// Requires a confirmation
				const entity = current.table.fnGetData($(this).closest('tr')[0]);
				bootbox.confirmDelete(function (confirmed) {
					confirmed && current.deleteUser(entity.id, entity.firstName + ' ' + entity.lastName);
				}, entity.id + ' [' + current.$main.getFullName(entity) + ']');
			}
		},

		/**
		 * Reset the selected user password after popup confirmation, or directly from its identifier.
		 */
		resetUserPassword: function (id, name) {
			current.confirmUserOperation($(this), id, name, 'reset', 'reseted', 'PUT');
		},

		/**
		 * Lock the selected user after popup confirmation, or directly from its identifier.
		 */
		lockUser: function (id, name) {
			current.confirmUserOperation($(this), id, name, 'lock', 'locked', 'DELETE');
		},
		/**
		 * Isolate the selected user after popup confirmation, or directly from its identifier.
		 */
		isolateUser: function (id, name) {
			current.confirmUserOperation($(this), id, name, 'isolate', 'isolated', 'DELETE');
		},

		/**
		 * Unlock the selected user.
		 */
		unlockUser: function () {
			current.userOperation($(this), 'unlock', 'unlocked');
		},
		/**
		 * Restore the selected user.
		 */
		restoreUser: function () {
			current.userOperation($(this), 'restore', 'restored');
		},

		/**
		 * Disable/Lock the selected user after popup confirmation, or directly from its identifier.
		 */
		confirmUserOperation: function ($item, id, name, operation, operated, method) {
			if ((typeof id) === 'string') {
				// Process without confirmation
				$.ajax({
					type: method,
					contentType: 'text/plain',
					url: REST_PATH + 'service/id/user/' + id + '/' + operation,
					success: function (data) {
						notifyManager.notify(Handlebars.compile(current.$messages[operated + '-confirm'])(name));
						current.table && current.table.api().ajax.reload();
						if (operation === 'reset') {
							// Callback the popup to display the generated password
							_('generated-password').val(data);
							_('rest-password-popup').modal('show');
						}
					}
				});
			} else {
				// Requires a confirmation
				const entity = current.table.fnGetData($item.closest('tr')[0]);
				bootbox.confirm(function (confirmed) {
					confirmed && current.confirmUserOperation($item, entity.id, entity.firstName + ' ' + entity.lastName, operation, operated, method);
				}, current.$messages[operation], Handlebars.compile(current.$messages[operation + '-confirm'])(entity.id + ' [' + current.$main.getFullName(entity) + ']'), current.$messages[operation]);
			}
		},

		/**
		 * Enable/Unlock the selected user.
		 */
		userOperation: function ($item, operation, operated) {
			const entity = current.table.fnGetData($item.closest('tr')[0]);
			const id = entity.id;
			const name = entity.firstName + ' ' + entity.lastName;
			$.ajax({
				type: 'PUT',
				url: REST_PATH + 'service/id/user/' + id + '/' + operation,
				success: function () {
					notifyManager.notify(Handlebars.compile(current.$messages[operated + '-confirm'])(name));
					current.table && current.table.api().ajax.reload();
				}
			});
		}
	};
	return current;
});
