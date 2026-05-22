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
  // Action de ligne d'abonnement, contribué via renderFeatures.
  'id.renderFeatures.manage': 'Gérer les membres',
  // Détail de ligne d'abonnement, contribué via renderDetailsKey.
  'id.renderDetailsKey.members': 'Membres',
}
