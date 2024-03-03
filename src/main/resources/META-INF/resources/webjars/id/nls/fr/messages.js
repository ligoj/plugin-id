/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
define({
	'title': 'Identité',
	'company': 'Société',
	'delegate': 'Délégation',
	'groups': 'Groupes',
	'group': 'Groupe',
	'department': 'Département',
	'localid': 'Identifiant local',
	'container-scope': 'Étendue de conteneur',
	'user': 'Utilisateur',
	'admin': 'Administration',
	'write': 'Écriture',
    'admin-help': 'Avec le niveau de sécurité d\'administration sur cette ressource, les receveurs de cette délégation peuvent créer d\'autres délégation pour partager cet accès avec d\'autres receveurs valides',
    'write-help': 'Avec le niveau de sécurité d\'écriture, les receveurs de cette délégation peuvent modifier les membres des groupes impliqués. Sans cet accès cette délégation ne donne qu\'un droit de lecture',
	'agreement': 'Convention d\'usage',
	'agree': 'J\'ai lu et j\'accepte les conditions d\'utilisation',
    'delay-message': 'Certains outils tels que JIRA nécessitent une synchronisation périodique avec un délais d\'au plus 1h.',
	'locked': 'Verrouillé',
	'quarantine': 'Quarantaine',
	'agreement-details': '<div>La gestion des utilisateurs et des droits associés sont au cœur des accès aux outils.</div><br /><div>Il est donc important de respecter ces quelques règles d\'usage :<ul><li>Les comptes créés doivent correspondre à des personnes physiques et sont personnels; donc pas de compte générique ou partagé.</li><li>Lors du départ d\'une personne de la société, son compte doit être supprimé immédiatement. Le manquement à cette obligation entrave la sécurité du système d\'information de la société: une personne en dehors de la société sera capable d\'accéder à des informations sensibles.</li><li>Lors du départ d\'une personne d\'un groupe (Département, Projet,...), elle doit être retirée de ce groupe. Le manquement à cette obligation entrave la sécurité de l\'entité organisationnelle associée.</li><li>Les mails des comptes créés doivent correspondre à un contexte professionnel; pas de <code>yahoo.fr</code>, <code>gmail.com</code>, etc.</li></ul></div>',
	'agreement-accepted': 'Vous avez accepté les <a id="showAgreement">règles d\'usage</a> des comptes.',
	'sample': 'Exemple',
	'error': {
		'last-member-of-group': 'L\'utilisateur {{user}} ne peut être supprimé, il est le dernier membre du groupe {{group}}',
		'locked': 'Ce conteneur est verrouillé, il ne peut être supprimé ou modifié',
		'read-only': '{{[1]}} cannot be updated',
		'container-scope-match': 'Etendue attendu est {{provided}} et le type fourni est {{scope}}'
	}
});
