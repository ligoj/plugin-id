/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define(function () {
	const current = {

		/**
		 * Flag objects
		 */
		table: null,
		search: false,

		/**
		 * Edited delegates' identifier
		 */
		currentId: 0,

		initialize: function (parameters) {
			current.onHashChange(parameters);
		},

		/**
		 * Manage internal navigation.
		 */
		onHashChange: function (/* parameter */) {
			// Search mode
			delete current.currentId;
			current.initializeSearch();
		},

		/**
		 * Initialize the search UI components
		 */
		initializeSearch: function () {
			if (current.search) {
				return;
			}
			current.search = true;
			_('confirmCreate').click(current.save);

			// Delegate edition pop-up
			_('popup').on('shown.bs.modal', function () {
				_('receiver').select2('focus');
			}).on('show.bs.modal', function (event) {
				validationManager.reset($(this));
				validationManager.mapping.name = 'resource';
				const $source = $(event.relatedTarget);
				const $tr = $source.closest('tr');
				let uc = ($tr.length && current.table.fnGetData($tr[0])) || null;
				uc = uc?.id ? uc : {};
				current.currentId = uc.id;
				_('canAdmin').prop('checked', uc.canAdmin || false);
				_('canWrite').prop('checked', uc.canWrite || false);
				current.updateDropdownIcon('receiver', uc.receiverType || 'user', uc.receiver?.name);
				current.updateDropdownIcon('resource', uc.type || 'group', uc.name);
			}).on('click', '.dropdown-menu [data-type]', function() {
				// Toggle selection
				current.updateDropdownIcon($(this).closest('[data-group]').attr('data-group'), $(this).attr('data-type'));
			});

			// Global datatables filter
			_('search').on('keyup', function () {
				current.table?.fnFilter($(this).val());
			});

			// Display the right component for selected type
			$('#type').on('change', current.synchronizeObjectStatus);

			$('.search-type').on('click', function () {
				$(this).addClass('active').siblings().removeClass('active');
				const type = $(this).val();
				current.table.fnSettings().ajax = REST_PATH + 'security/delegate' + (type ? '?type=' + type.toUpperCase() : '');
				current.table.api().ajax.reload();
			});

			// Also initialize the datatables component
			current.initializeDataTable();
		},

		/**
		 * Synchronize the visible inputs and related UI depending on the selected type.
		 * @param inputId {string} Identifier of input determining the resource or the receiver. Used to find the couple "type/input"
		 */
		updateDropdownIcon: function (inputId, type, value) {
			const $input = _(inputId);
			const $group = $input.closest('.form-group');

			// Update the selected drop down item
			$group.find('.dropdown-menu [data-type].active').removeClass('active');
			$group.find('.dropdown-menu [data-type="'+type+'"]').addClass('active');

			// Update the drop down icon
			$group.find('.dropdown-toggle i').attr('class', current.$main.targetTypeClass[type] + ' fa-fw');

			// Invalidate the previous select2
			$input.removeAttr('placeholder').select2('destroy');
			const select2Init = current.$main['newSelect2'+type.capitalize()];
			if (typeof select2Init === 'function') {
				// Select2 input available, build it and push the right data/value
				select2Init($input, (type === 'group' || type === 'company') ? '/admin' : undefined).select2(typeof value === 'object' ? 'data' : 'val', value || null);
			} else {
				// Simple text
				$input.val(value || null).attr('placeholder', current.$messages.tree);
			}
		},

		showPopup: function (context) {
			_('popup').modal('show', context);
		},

		/**
		 * Initialize the delegates datatables (server AJAX)
		 */
		initializeDataTable: function () {
			current.table = _('table').dataTable({
				dom: 'rt<"row"<"col-xs-6"i><"col-xs-6"p>>',
				serverSide: true,
				searching: true,
				ajax: REST_PATH + 'security/delegate',
				createdRow: function (nRow) {
					$(nRow).find('.update').on('click', function () {
						current.$parent.requireAgreement(current.showPopup, $(this));
					});
					$(nRow).find('.delete').on('click', current.deleteDelegate);
				},
				columns: [
					{
						data: 'receiver',
						render: {
						    display : (_i, _mode, data) => current.$main.getResourceLink(data.receiver, data.receiverType),
						    _: value => value.id
						}
					}, {
						data: 'resource',
						className: 'hidden-xs truncate',
						render: {
                            display : (_i, _mode, data) => current.$main.getResourceLink(data.name, data.type),
                            _: value => data.name
                        }
					}, {
						data: 'canAdmin',
						width: '16px',
						render: {
                            display : value => value ? '<i class="fas fa-check"></i>' : '&nbsp;',
                        }
					}, {
						data: 'canWrite',
						width: '16px',
						render: {
                            display : value => value ? '<i class="fas fa-check"></i>' : '&nbsp;',
                        }
					}, {
						data: 'managed',
						width: '32px',
						orderable: false,
						render: function (value) {
							if (value) {
								const editLink = `<a class="update"><i class="fas fa-pencil-alt" data-toggle="tooltip" title="${current.$messages.update}"></i></a>`;
								return editLink + `<a class="delete"><i class="fas fa-times" data-toggle="tooltip" title="${current.$messages.delete}"></i></a>`;
							}
							return '&nbsp;';
						}
					}
				],
				buttons: [
					{
						extend: 'create',
						action: function () {
							current.$parent.requireAgreement(current.showPopup);
						}
					}
				]
			});
		},

		formToObject: function () {
			const $popup = _('popup');
			const result = {
				id: current.currentId,
				receiver: _('receiver').val(),
				receiverType: $popup.find('[data-group="receiver"]').find('[data-type].active').attr('data-type'),
				name: _('resource').val(),
				type: $popup.find('[data-group="resource"]').find('[data-type].active').attr('data-type'),
				canAdmin: $('#canAdmin:checked').length === 1 || null,
				canWrite: $('#canWrite:checked').length === 1 || null
			};
			// Trim the data
			Object.keys(result).forEach(function(key) {
				(result[key] === null || result[key] === '') && delete result[key];
			});

			return result;
		},

		save: function () {
			const data = current.formToObject();
			$.ajax({
				type: current.currentId ? 'PUT' : 'POST',
				url: REST_PATH + 'security/delegate',
				dataType: 'json',
				contentType: 'application/json',
				data: JSON.stringify(data),
				success: function (data) {
					notifyManager.notify(Handlebars.compile(current.$messages[current.currentId ? 'updated' : 'created'])(current.currentId || data));
					current.table?.api().ajax.reload();
					_('popup').modal('hide');
				}
			});
		},

		/**
		 * Delete the selected delegate after popup confirmation, or directly from its identifier.
		 */
		deleteDelegate: function (id, text) {
			if ((typeof id) === 'number') {
				// Delete without confirmation
				$.ajax({
					type: 'DELETE',
					url: REST_PATH + 'security/delegate/' + id,
					success: function () {
						notifyManager.notify(Handlebars.compile(current.$messages.deleted)(text + '(' + id + ')'));
						current.table?.api().ajax.reload();
					}
				});
			} else {
				// Requires a confirmation
				const entity = current.table.fnGetData($(this).closest('tr')[0]);
				const display = current.$main.getResourceLink(entity.receiver, entity.receiverType) + ' <i class="fas fa-arrow-right"></i> ' + current.$main.getResourceLink(entity.name, entity.type);
				bootbox.confirmDelete(function (confirmed) {
					confirmed && current.deleteDelegate(entity.id, display);
				}, display);
			}
		}
	};
	return current;
});
