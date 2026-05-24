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
  // Subscription row actions contributed via renderFeatures.
  'id.renderFeatures.manage': 'Manage members',
  'id.renderFeatures.help': 'Documentation',
  // Subscription row details: stable "key" (group name) + live
  // "features" (member count). Mirrors legacy renderDetailsKey /
  // renderDetailsFeatures split.
  'id.renderDetailsKey.group': 'Group',
  'id.renderDetailsFeatures.members': 'Members',
  // Group members management view (ported from legacy id.html).
  'id.group.unknown': '(unknown group)',
  'id.group.subtitle': 'Members of this group inherit the subscription\'s permissions.',
  'id.group.addPlaceholder': 'Search a user to add',
  'id.group.add': 'Add',
  'id.group.addedToast': 'Added {user} to {group}',
  'id.group.removeTitle': 'Remove member',
  'id.group.removeConfirm': 'Remove {user} from group {group}?',
  'id.group.removedToast': 'Removed {user} from {group}',
  'id.group.transitive': 'Indirect member through a sub-group — manage them on the parent.',
}
