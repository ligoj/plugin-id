/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define({
	root: {
		'title': 'User management',
		'user-clean-message': 'Spaces around inputs will be removed and names will be fully capitalized',
		'user-import-full-message': 'Import will update existing users, and create the others.',
		'user-import-atomic-message': 'Import will execute one alter or operation per record.',
		'user-import-dry-run': 'Dry run',
		'user-import-full': 'Full',
		'user-import-atomic': 'Atomic',
		'create-another': 'Create another user',
		'warn-mail-perso': 'Account mail must be a professional mail; this excludes yahoo.fr, gmail.com, etc.',
		'created-account': 'Account {{id}} has been created and an email has been sent.</br><i class="fas fa-envelope"></i> <a href="mailto:{{mail}}?subject=New account {{id}}">{{mail}}</a>',
		'employer': 'Employer',
		'member': 'Member',
		'company-help': 'Only companies you manage are available there',
		'group-help': 'Only groups you manage are available there',
		'unlock': 'Unlock',
		'unlocked-confirm': '{{this}} has been unlocked. This user must recover a new password.',
		'lock': 'Lock',
		'lock-confirm': 'Are you sure to lock {{this}}?<br>This user will neither be able to authenticate anymore neither request a new password, but still be visible by the tools and will continue to receive some mails from them.<br>In addition, the system clears the password, and it will not restore this password when unlocking the user.',
		'locked-confirm': '{{this}} has been locked. Password has been cleared',
		'restore': 'Restore',
		'restored-confirm': '{{this}} has been restored. This user must recover a new password.',
		'isolate': 'Isolate',
		'isolated': 'Isolated',
		'isolate-confirm': 'Are you sure to isolate {{this}}?<br>This account will be locked and will not be more visible by the tools.<br>This user will neither be able to authenticate anymore, neither request a new password, neither receive mails from the tools.<br>In addition, the system clears the password, and it will not restore this password when unlocking the user.',
		'isolated-confirm': '{{this}} has been isolated. Password has been cleared',
		'reset': 'Password Reset',
		'reset-confirm': 'Are you sure to reset the password of {{this}}?<br>An email will be sent to this user and you.',
		'reset-ok': '{{this}} password has been reset.',
		'show-password' : 'Show password',
		'password' : 'Password',
		'quiet': 'Silent',
		'quiet-help': 'Silent import does not imply notification related to this import'
	},
	fr: true
});
