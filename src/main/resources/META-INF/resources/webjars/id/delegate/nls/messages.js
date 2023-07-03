/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define({
	root: {
		'title': 'Organizational Delegate',
		'delegate': 'Delegate',
		'resourceType': 'Resource type',
		'resource': 'Resource',
        'resource-help' : 'A resource within a delegate is the visible group, company or organizational tree, that you want to share with a receiver. Only resources with administration access levels can be shared.',
		'receiverType' : 'Receiver type',
		'receiver' : 'Receiver',
    	'receiver-help' : 'A receiver within a delegate is the visible group, company or user, that will get the right to see, and optionally modify and share the resource.',
		'delegate-message': 'The delegates allow users, groups and companies to share a part of their rights to other receivers',
		'tree': 'Tree',
		'delegate-audience': {
			'receiver-user' : 'User {{{[0]}} will be able to ',
			'receiver-group' : 'Members of group {{{[0]}} and its sub-groups (currently {{[1]}}) will be able to ',
			'receiver-company' : 'Users within the company {{{[0]}}} and its sub-companies (currently {{[1]}} companies and {{[2]}} users) will be able to ',
			'to-see' : 'see the ',
			'to-write' : 'see and update the ',
			'to-admin' : ' share this right, and ',
			'to-admin-write' : ' share this right, ',
			'resource-group' : ' members of the group {{[0]}} and its sub-groups, currently {{[1]}} groups',
			'resource-company' : ' users within the company {{[0]}} and its sub-companies, currently {{[1]}} companies and {{[2]}} users',
			'resource-tree' : ' users and groups within the tree {{[0]}}, currently {{[1]}} groups, {{[2]}} companies and {{[3]}} users'
		}
	},
	fr: true
});
