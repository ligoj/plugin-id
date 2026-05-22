// Plugin-local translations merged into the host i18n store at install
// time. Keep keys flat (dot-separated) to match the host's existing
// convention.
export default {
  'delegate.type.user': 'User',
  'delegate.type.group': 'Group',
  'delegate.type.company': 'Company',
  'delegate.type.tree': 'Tree',
  'delegate.resourceDnHint': 'LDAP DN of the subtree (e.g. ou=projects,dc=acme,dc=com)',
  'user.deleteConfirmBefore': 'Are you sure you want to delete ',
  'user.deleteConfirmAfter': '?',
  'group.deleteConfirmBefore': 'Are you sure you want to delete ',
  'group.deleteConfirmAfter': '?',
  'company.deleteConfirmBefore': 'Are you sure you want to delete ',
  'company.deleteConfirmAfter': '?',
  // Subscription row action contributed via renderFeatures.
  'id.renderFeatures.manage': 'Manage members',
  // Subscription row details contributed via renderDetailsKey.
  'id.renderDetailsKey.members': 'Members',
}
