/**
 * Manager used to populate and manage IAM features.
 *
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define(function () {
	const current = {
		initialize: function () {
			_('showAgreement').on('click', function () {
				_('agree').prop('checked', true).attr('disabled', 'disabled');
				_('agreementPopup').find('[type="submit"]').addClass('hide');
				_('agreementPopup').modal('show');
			});
			if (current.$session.userSettings['security-agreement']) {
				_('acceptedAgreement').removeClass('hide');
			}
		},

		/**
		 * Require agreement popup.
		 * @param context Optional context to restore for popup
		 */
		requireAgreement: function (target, context) {
			if (current.$session.userSettings['security-agreement']) {
				// Agreement already accepted
				target(context);
			} else {
				// Show the agreement popup
				_('agree').prop('checked', false).removeAttr('disabled');
				_('agreementPopup').find('[type="submit"]').removeClass('hide');
				_('agreementPopup').off('submit').on('submit', function (e) {
					e.preventDefault();
					if ($('#agree:checked').length === 1) {
						$.ajax({
							type: 'POST',
							url: REST_PATH + 'system/setting/security-agreement/1',
							dataType: 'json',
							contentType: 'application/json'
						});
						current.$session.userSettings['security-agreement'] = true;
						_('agreementPopup').modal('hide');

						// Just accepted
						target(context);
					}
					return false;
				}).modal('show');
			}
		},

		/**
		 * Interval identifier for the refresh
		 */
		intervalVariable: null,

		/**
		 * Monitor the import.
		 * @param Integer id : import identifier
		 */
		unscheduleUploadStep: function () {
			clearInterval(current.intervalVariable);
		},

		scheduleUploadStep: function (url, id, callback) {
			current.intervalVariable = setInterval(function () {
				current.synchronizeUploadStep(url, id, callback);
			}, 1000);
		},

		synchronizeUploadStep: function (url, id, callback) {
			current.unscheduleUploadStep();
			$.ajax({
				dataType: 'json',
				url: REST_PATH + url + '/' + id + '/status',
				type: 'GET',
				success: function (data) {
					current.displayUploadPartialResult(data);
					if (data.end) {
						current.displayUploadResult(url, id, callback);
						return;
					}
					current.scheduleUploadStep(url, id, callback);
				}
			});
		},

		/**
		 * Display the complete result.
		 */
		displayUploadResult: function (url, id, callback) {
			$.ajax({
				dataType: 'json',
				url: REST_PATH + url + '/' + id,
				type: 'GET',
				success: function (data) {
					current.displayUploadPartialResult(data.status);
					$('.import-progress').removeClass('progress-bar-striped').html(current.$messages.finished);
					const $summary = $('.import-summary');
					const errors = data.entries.filter(e => e.statusText).map(e => `${(e.id || e.user)} : ${errorManager.manageBadRequestError(e.statusText)}`);
					if (errors.length) {
						// At least one error to display
						$summary.append(`<br>Errors (${errors.length}): ${errors.join('<br>&nbsp;')}`);
					}
					$summary.removeClass('alert-info').addClass(errors ? 'alert-danger' : 'alert-success');
					if (callback) {
						callback(data);
					}
				}
			});
		},

		/**
		 * Display the partial result.
		 */
		displayUploadPartialResult: function (data) {
			$('.import-progress').attr('aria-valuenow', data.done).css('width', (data.done * 100 / data.entries) + '%').html(data.done + '/' + data.entries);
			$('.import-summary').html('Started : ' + moment(data.start).format('LTS') + '<br>Finished : ' + moment(data.end).format('LTS') + '<br>Entries : ' + data.entries + '<br>Done : ' + data.done + '<br>Status : ' + data.status + '<br>statusText : ' + (data.statusText || '-'));
		}
	};
	return current;
});
