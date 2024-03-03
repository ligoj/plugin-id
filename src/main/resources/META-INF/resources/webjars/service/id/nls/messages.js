/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define({
	root: {
		'login': 'Login',
		'mail': 'Mail',
		'groups': 'Groups',
		'service:id': 'Identity',
		'service:id:uid-pattern': 'User id pattern',
		'service:id:uid-pattern-description': 'User identifier pattern validating an authentication',
		'service:id:group': 'Group',
		'service:id:parent-group': 'Parent group',
		'service:id:parent-group-description': 'Optional parent group where the new group will be added',
		'service:id:ou': 'Organization',
		'service:id:ou-description': 'Organizational Unit or customer, used as prefix for the full group name. Will be created if it does not exist.',
		'service:id:ou-not-exists': 'Typed organization does not exist yet and will be created. Are you sure about the syntax?',
		'service:id:group-simple-name': 'Simple name',
		'service:id:group-simple-name-description': 'Simple group name without organisation prefix',
		'service:id:activity-project': 'Project activity report',
		'service:id:activity-group': 'Group activity report',
		'service:id:add-member': 'Add this user to the current group',
		'service:id:added-member': 'User {{[0]}} has been added to group {{[1]}}',
		'service:id:group-manage': 'Manage team',
		'service:id:remove-member': 'Remove this user to the current group',
		'service:id:removed-member': 'User {{[0]}} has been removed from group {{[1]}}',
		'service:id:sub-group-help': 'Inherited membership.<br/>This users belongs to one of child groups of the current group. The user cannot be removed there from this group, and when removed from all sub-groups won\'t appear anymore there.'
	},
	fr: true
});
