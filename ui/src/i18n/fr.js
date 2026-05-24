// Plugin-local translations merged into the host i18n store at install
// time. Keep keys flat (dot-separated) to match the host's existing
// convention.
//
// Voir en.js pour la rationale : les clés `service:id:*` sont des
// libellés de paramètres possédés par plugin-id et hérités par tous
// ses sous-plugins (plugin-id-ldap, plugin-id-sql, …) ; l'assistant
// d'abonnement les résout via paramLabel()/paramHint().
export default {
  // Libellés de paramètres hérités (possédés par plugin-id).
  'service:id': 'Identité',
  'service:id:group': 'Groupe',
  'service:id:parent-group': 'Groupe parent',
  'service:id:parent-group-description': 'Groupe parent optionnel qui contiendra le nouveau groupe créé',
  'service:id:ou': 'Organisation',
  'service:id:ou-description': 'Unité d\'organisation, ou client, utilisée comme préfixe au nom complet du groupe. Sera créée si elle n\'existe pas.',
  'service:id:ou-not-exists': 'L\'organisation saisie n\'existe pas encore et sera créée. Êtes-vous certain de la syntaxe ?',
  'service:id:uid-pattern': 'Motif d\'utilisateur',
  'service:id:uid-pattern-description': 'Motif de validation de l\'identifiant utilisateur pour accepter une authentification',
  'service:id:group-simple-name': 'Nom simple',
  'service:id:group-simple-name-description': 'Nom simple du groupe, sans le préfixe d\'organisation',

  'delegate.type.user': 'Utilisateur',
  'delegate.type.group': 'Groupe',
  'delegate.type.company': 'Entité',
  'delegate.type.tree': 'Arborescence',
  'delegate.resourceDnHint': 'DN LDAP du sous-arbre (ex. ou=projects,dc=acme,dc=com)',
  'user.deleteConfirmBefore': 'Êtes-vous certain de supprimer ',
  'user.deleteConfirmAfter': ' ?',
  'group.deleteConfirmBefore': 'Êtes-vous certain de supprimer ',
  'group.deleteConfirmAfter': ' ?',
  'company.deleteConfirmBefore': 'Êtes-vous certain de supprimer ',
  'company.deleteConfirmAfter': ' ?',
  // Actions de ligne d'abonnement, contribuées via renderFeatures.
  'id.renderFeatures.manage': 'Gérer les membres',
  'id.renderFeatures.help': 'Documentation',
  // Détails de ligne d'abonnement : « clé » stable (nom du groupe) +
  // « features » live (nombre de membres).
  'id.renderDetailsKey.group': 'Groupe',
  'id.renderDetailsFeatures.members': 'Membres',
  // Vue de gestion des membres d'un groupe (portage de id.html legacy).
  'id.group.unknown': '(groupe inconnu)',
  'id.group.subtitle': 'Les membres de ce groupe héritent des permissions de la souscription.',
  'id.group.addPlaceholder': 'Rechercher un utilisateur à ajouter',
  'id.group.add': 'Ajouter',
  'id.group.addedToast': '{user} ajouté à {group}',
  'id.group.removeTitle': 'Retirer un membre',
  'id.group.removeConfirm': 'Retirer {user} du groupe {group} ?',
  'id.group.removedToast': '{user} retiré de {group}',
  'id.group.transitive': 'Membre indirect via un sous-groupe — à gérer depuis le groupe parent.',
}
