// Plugin-local translations merged into the host i18n store at install
// time. Keep keys flat (dot-separated) to match the host's existing
// convention.
//
// Two families live here:
//  1. `service:id:*` keys — labels (and `-description` hints) for the
//     parameters this plugin OWNS. The subscribe wizard auto-resolves
//     `t(parameter.id)` against the host store, so the same keys serve
//     both the parent's own parameters (`service:id:group`, …) and any
//     tool-level plugin that inherits them (e.g. `plugin-id-ldap`'s
//     subscriptions also carry `service:id:ou`, `service:id:parent-group`).
//  2. Vue-only keys (dotted: `id.renderFeatures.manage`, `id.group.*`) —
//     used by the rewritten components.
export default {
  // Inherited parameter labels (owned by plugin-id). Picked up by the
  // wizard via paramLabel()/paramHint() for ANY subscription whose tool
  // is below `service:id` — including LDAP, SQL, and future siblings.
  'service:id': 'Identity',
  'service:id:group': 'Group',
  'service:id:parent-group': 'Parent group',
  'service:id:parent-group-description': 'Optional parent group where the new group will be added',
  'service:id:ou': 'Organization',
  'service:id:ou-description': 'Organizational Unit or customer, used as prefix for the full group name. Will be created if it does not exist.',
  'service:id:ou-not-exists': 'Typed organization does not exist yet and will be created. Are you sure about the syntax?',
  'service:id:uid-pattern': 'User id pattern',
  'service:id:uid-pattern-description': 'User identifier pattern validating an authentication',
  'service:id:group-simple-name': 'Simple name',
  'service:id:group-simple-name-description': 'Simple group name without organisation prefix',

  'delegate.type.user': 'User',
  'delegate.type.group': 'Group',
  'delegate.type.company': 'Company',
  'delegate.type.tree': 'Tree',
  'delegate.resourceDnHint': 'LDAP DN of the subtree (e.g. ou=project,dc=acme,dc=com)',
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
