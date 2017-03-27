define(function () {
	var current = {

		/**
		 * Member table
		 */
		table: null,

		/**
		 * Current configuration.
		 */
		model: null,

		/**
		 * Current group.
		 */
		group: null,

		/**
		 * Show the members of the given group
		 */
		configure: function (configuration) {
			current.model = configuration;
			current.group = configuration.parameters['service:id:group'];
			current.initializeForm();
			current.initializeDataTable();
			current.table.api().ajax.reload();
			_('group-name').text(current.group);
			_('add-user').select2('val', null);
			_('subscribe-configuration-id').removeClass('hide');
		},

		initializeForm: function () {
			_('search-user').select2({
				minimumInputLength: 0,
				formatSelection: function (object) {
					return object.id + ' [<small>' + current.$super('getFullName')(object) + '</small>]';
				},
				escapeMarkup: function (m) {
					return m;
				},
				id: function (object) {
					return object.id;
				},
				ajax: {
					url: REST_PATH + 'service/id/user',
					dataType: 'json',
					data: function (term, page) {
						return {
							'search[value]': term,
							rows: 15,
							page: page,
							sidx: 'name',
							sord: 'asc'
						};
					},
					results: function (data, page) {
						var result = [];
						$(data.data).each(function () {
							this.text = this.id + ' [<small>' + current.$super('getFullName')(this) + '</small>]';
							result.push(this);
						});
						return {
							more: data.recordsFiltered > page * 10,
							results: result
						};
					}
				}
			}).on('change', function (e) {
				// Member might be selected, enable the 'add' button
				if (e.added && e.added.id) {
					// Enable the button
					_('add-user').removeAttr('disabled');
				} else {
					// Disable the button
					_('add-user').attr('disabled', 'disabled');
				}
			});

			// Add the selected user to the current group
			_('add-user').on('click', function () {
				var user = _('search-user').val();
				var group = current.group;

				// Clear the selection
				_('search-user').select2('val', '');
				_('add-user').attr('disabled', 'disabled');

				// Proceed
				$.ajax({
					dataType: 'json',
					url: REST_PATH + 'service/id/user/' + encodeURIComponent(user) + '/group/' + encodeURIComponent(group),
					type: 'PUT',
					success: function () {
						notifyManager.notify(Handlebars.compile(current.$messages['service:id:added-member'])([user, group]));
						current.table && current.table.api().ajax.reload();
					}
				});
			});

			// Global datatables filter
			_('subscribe-configuration-id-search').on('keyup', function () {
				current.table && current.table.fnFilter($(this).val());
			});

			// Remove the selected user from the current group
			_('members-table').on('click', '.remove-user', function () {
				var user = current.table.fnGetData($(this).closest('tr')[0]).id;
				var group = current.group;
				$.ajax({
					dataType: 'json',
					url: REST_PATH + 'service/id/user/' + encodeURIComponent(user) + '/group/' + encodeURIComponent(group),
					type: 'DELETE',
					success: function () {
						notifyManager.notify(Handlebars.compile(current.$messages['service:id:removed-member'])([user, group]));
						current.table && current.table.api().ajax.reload();
					}
				});
			});
		},

		/**
		 * Initialize the users datatables (server AJAX)
		 */
		initializeDataTable: function () {
			current.table = _('members-table').dataTable({
				dom: 'rt<"row"<"col-xs-6"i><"col-xs-6"p>>',
				serverSide: true,
				destroy: true,
				searching: true,
				ajax: function () {
					return REST_PATH + 'service/id/user?group=' + encodeURIComponent(current.group);
				},
				columns: [{
					data: 'id',
					width: '80px',
					render: function (_i, _j, data) {
						return current.$super('getUserLoginLink')(data);
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
					orderable: false,
					className: 'hidden-xs hidden-sm hidden-md truncate'
				}, {
					data: 'groups',
					orderable: false,
					className: 'hidden-xs hidden-sm truncate',
					render: function (_i, _j, data) {
						var groups = [];
						$(data.groups).each(function () {
							groups.push(this.name);
						});
						return groups;
					}
				}, {
					data: null,
					width: '16px',
					orderable: false,
					render: function () {
						return '<a class="remove-user"><i class="fa fa-user-times" data-toggle="tooltip" title="' + current.$messages['service:id:remove-member'] + '"></i></a>';
					}
				}]
			});
		}
	};
	return current;
});
