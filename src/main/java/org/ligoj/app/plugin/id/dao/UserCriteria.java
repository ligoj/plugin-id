/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Strings;
import org.ligoj.app.iam.UserOrg;

import lombok.experimental.UtilityClass;

/**
 * Free-text criterion of the user lookups ({@code search[value]}), shared by the identity repositories: a
 * case-insensitive "contains" on the login, the first name, the last name, the first mail, and every custom
 * attribute value — so the visual identifier ({@code service:id:visual-id-name}, possibly a custom attribute)
 * displayed by the UI is always searchable.
 */
@UtilityClass
public class UserCriteria {

	/**
	 * Indicates the user matches the criterion.
	 *
	 * @param user     The user.
	 * @param criteria The criterion, <code>null</code> or blank matches every user.
	 * @return <code>true</code> when one of the searchable attributes contains the criterion.
	 */
	public static boolean matches(final UserOrg user, final String criteria) {
		if (criteria == null || criteria.isBlank()) {
			return true;
		}
		return Strings.CI.contains(user.getId(), criteria) || Strings.CI.contains(user.getFirstName(), criteria)
				|| Strings.CI.contains(user.getLastName(), criteria)
				|| CollectionUtils.isNotEmpty(user.getMails()) && Strings.CI.contains(user.getMails().getFirst(), criteria)
				|| user.getCustomAttributes() != null
						&& user.getCustomAttributes().values().stream().anyMatch(v -> Strings.CI.contains(v, criteria));
	}
}
