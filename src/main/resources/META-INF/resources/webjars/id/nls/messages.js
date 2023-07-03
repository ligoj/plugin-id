/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define({
	root: {
		'title': 'Identity',
		'company': 'Company',
		'delegate': 'Delegate',
		'login': 'Login',
		'mail': 'Mail',
		'groups': 'Groups',
		'group': 'Group',
		'department': 'Department',
		'localid': 'Local identifier',
		'container-scope': 'Container scope',
		'user': 'User',
		'admin': 'Administration',
		'write': 'Write',
		'admin-help': 'With administration security level on this resource, receivers can create another delegate to share this access with other valid receivers',
		'write-help': 'With write security level on this resource, receivers can modify the group members. Without this access the right is read only',
		'type': 'Type',
		'agreement': 'Usage Agreement',
		'agree': 'I\'ve read and I accept this agreement',
		'delay-message': 'Some tools like JIRA need a periodical synchronization with a delay up to 1h.',
		'locked': 'Locked',
		'quarantine': 'Quarantine',
		'agreement-details': '<div>The user management and the associated rights are the core of all access to tools.</div><br/><div>It\'s important to respect some usage rules :<ul><li>Created accounts must match to physical persons and are personal; so, nor shared, nor generic accounts.</li><li>Upon departure of a person from a company, his or her account must be immediately deleted. Forgetting this action implies severe security issues for the whole information system: a foreign of the company would be able to access to a sensitive information.</li><li>Upon the departure from a group (Business Unit, Department, Project,...), he or she must be removed from this group. Forgetting this action implies security issues for the associated organizational unit.</li><li>The created mails of accounts must suit to a professional frame; no <code>yahoo.fr</code>, <code>gmail.com</code>, etc.</li></ul></div>',
		'agreement-accepted': 'You have accepted the account <a id="showAgreement">usage rules</a>.',
		'sample': 'Sample',
		'error': {
			'last-member-of-group': 'You can\'t delete user {{user}}, he is the last member of group {{group}}',
			'locked': 'This container is locked, you cannot create or delete it',
			'read-only': '{{[1]}} cannot be updated',
			'container-scope-match': 'Expected scope is {{expected}} and you provided {{scope}}'
		}
	},
	fr: true
});
