/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define({
	'groups': 'Groupes',
	'service:id': 'Identité',
	'service:id:uid-pattern': 'Pattern de capture de l\'identifiant d\'utilisateur dans un DN',
	'service:id:group': 'Groupe',
	'service:id:parent-group': 'Groupe parent',
	'service:id:parent-group-description': 'Groupe parent optionnel qui contiendra le nouveau groupe créé',
	'service:id:ou': 'Organisation',
	'service:id:ou-description': 'Unité d\'organisation, ou client, utilisée comme préfixe au nom complet du groupe. Sera créée si elle n\'existe pas.',
	'service:id:ou-not-exists': 'L\'organisation saisie n\'existe pas encore et sera créée. Etes-vous certain de la syntaxe?',
	'service:id:group-simple-name': 'Nom simple',
	'service:id:group-simple-name-description': 'Simple nom du groupe, sans le préfixe de l\'organisation',
	'service:id:activity-project': 'Rapport d\'activité du projet',
	'service:id:activity-group': 'Rapport d\'activité du groupe',
	'service:id:add-member': 'Ajouter cet utilisateur au groupe',
	'service:id:added-member': 'Utilisateur {{[0]}} a été ajouté au groupe {{[1]}}',
	'service:id:remove-member': 'Retirer cet utilisateur du groupe',
	'service:id:removed-member': 'Utilisateur {{[0]}} a été retiré du groupe {{[1]}}',
	'service:id:group-manage': 'Gestion de l\'équipe',
    'service:id:sub-group-help': 'Appartenance héritée.<br>Cet utilisateur appartient à l\'un des sous-groupes de ce groupe. Il ne peut être retiré ici de ce group, et lorsqu\'il sera retiré de tous les sou-groupes, n\'apparaitra plus ici.'
});
