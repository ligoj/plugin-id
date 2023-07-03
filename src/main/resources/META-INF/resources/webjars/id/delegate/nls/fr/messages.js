/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define({
	'title': 'Délégation Organisationnelle',
	'delegate': 'Délégation',
	'resourceType': 'Type de ressource',
	'resource': 'Ressource',
	'resource-help' : 'Une ressource d\'une délégation est le groupe, la société ou l\'arbre d\'organisation visible, que vous voulez partager avec un receveur. Seules les ressources avec le niveau d\'accès administration peuvent être partagées.',
	'receiverType' : 'Type de receveur',
	'receiver' : 'Receveur',
	'receiver-help' : 'Un receveur d\'une délégation est le groupe, la société ou l\'utilisateur visible, qui va recevoir les droits de voir et optionnellement modifier et partager la ressource.',
    'delegate-message': 'Les délégations permetent à des utilisateur, groupes et sociétés de partager une partie de leurs droits à d\'autres receveurs',
	'tree': 'Arbre',
	'delegate-audience': {
		'receiver-user' : 'Utilisateur {{{[0]}} sera capable de ',
		'receiver-group' : 'Les membres du groupe {{{[0]}} et ses sous-groupes (actuellement {{[1]}}) seront capables de ',
		'receiver-company' : 'Les utilisateur de la société {{{[0]}}} et ses sous-sociétés (actuellement {{[1]}} sociétés et {{[2]}} utilisateurs) seront capables de ',
		'to-see' : 'voir les ',
		'to-write' : 'voir et modifier les ',
		'to-admin' : ' partager ce droit, et ',
		'to-admin-write' : ' partager ce droit, ',
		'resource-group' : ' membre of the group {{[0]}} et ses sous-groupes, actuellement {{[1]}} groupes',
		'resource-company' : ' utilisateurs de la société {{[0]}} et ses sous-sociétés, actuellement {{[1]}} sociétés et {{[2]}} utilisateurs',
		'resource-tree' : ' utilisateurs et groupes de l\'arbre. {{[0]}}, actuellement {{[1]}} groupes, {{[2]}} sociétés et {{[3]}} utilisateurs'
	}
});
