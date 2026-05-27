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
  'delegate.resourceDnHint': 'DN LDAP du sous-arbre (ex. ou=project,dc=acme,dc=com)',
  'user.deleteConfirmBefore': 'Êtes-vous certain de supprimer ',
  'user.deleteConfirmAfter': ' ?',
  // Chantier D4 — saisie multi-email (v-combobox)
  'user.emailHint': 'Appuyez sur Entrée ou Tab pour valider chaque email',
  // Chantier D2 (rattrapage) — fragments encadrant le nombre d'éléments
  // en gras-rouge pour la suppression en masse.
  'common.bulkDeleteConfirmBefore': 'Supprimer ',
  'common.bulkDeleteConfirmAfter': ' éléments ? Cette action est irréversible.',
  // Chantier D2 — confirmations sensibles découpées en deux fragments
  // pour insérer l'identifiant en gras-rouge entre eux. Les clés
  // monolithiques `user.<action>Confirm` restent côté host pour
  // d'éventuels autres usages, on les surcharge plus.
  'user.lockConfirmBefore': 'Verrouiller l\'utilisateur ',
  'user.lockConfirmAfter': ' ? Il ne pourra plus se connecter.',
  'user.unlockConfirmBefore': 'Déverrouiller l\'utilisateur ',
  'user.unlockConfirmAfter': ' ? Il pourra à nouveau se connecter.',
  'user.isolateConfirmBefore': 'Isoler l\'utilisateur ',
  'user.isolateConfirmAfter': ' ? Cela supprimera toutes ses appartenances aux groupes.',
  'user.restoreConfirmBefore': 'Restaurer l\'utilisateur ',
  'user.restoreConfirmAfter': ' ?',
  'user.resetPasswordConfirmBefore': 'Réinitialiser le mot de passe de l\'utilisateur ',
  'user.resetPasswordConfirmAfter': ' ? Un nouveau mot de passe lui sera envoyé.',
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
  // Chantier D2 — fragments encadrant l'identifiant utilisateur en gras-rouge.
  'id.group.removeConfirmBefore': 'Retirer ',
  'id.group.removeConfirmAfter': ' du groupe {group} ?',
  'id.group.removedToast': '{user} retiré de {group}',
  'id.group.transitive': 'Membre indirect via un sous-groupe — à gérer depuis le groupe parent.',
}
