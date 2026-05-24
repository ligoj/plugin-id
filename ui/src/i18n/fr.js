// Plugin-local translations merged into the host i18n store at install
// time. Keep keys flat (dot-separated) to match the host's existing
// convention.
export default {
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
