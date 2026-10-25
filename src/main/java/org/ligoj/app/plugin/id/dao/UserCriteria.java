/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.util.Collection;

import org.ligoj.app.iam.UserOrg;

import lombok.experimental.UtilityClass;

/**
 * Free-text criterion of the user lookups ({@code search[value]}), shared by the identity repositories: a
 * case-insensitive "contains" on the login, the first name, the last name, the first mail, the company, the groups,
 * and every custom attribute value — so the visual identifier ({@code service:id:visual-id-name}, possibly a custom
 * attribute) and the company and groups displayed by the UI are always searchable.
 * <p>
 * Called for every user of a lookup: no stream, no lambda and no allocation. The case is ignored as
 * {@link String#regionMatches(boolean, int, String, int, int)} does.
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
		// The first char of the criterion, in both cases: the other chars are compared only from a matching one
		final var first = fold(criteria.charAt(0));
		final var upper = Character.toUpperCase(first);
		if (contains(user.getId(), criteria, first, upper) || contains(user.getFirstName(), criteria, first, upper)
				|| contains(user.getLastName(), criteria, first, upper)) {
			return true;
		}
		final var mails = user.getMails();
		if (mails != null && !mails.isEmpty() && contains(mails.getFirst(), criteria, first, upper)) {
			return true;
		}
		final var attributes = user.getCustomAttributes();
		return contains(user.getCompany(), criteria, first, upper) || containsAny(user.getGroups(), criteria, first, upper)
				|| attributes != null && containsAny(attributes.values(), criteria, first, upper);
	}

	/**
	 * Return the char compared by {@link String#regionMatches(boolean, int, String, int, int)}.
	 */
	private static char fold(final char c) {
		return Character.toLowerCase(Character.toUpperCase(c));
	}

	/**
	 * Indicates one of the values contains the criterion, ignoring the case.
	 */
	private static boolean containsAny(final Collection<String> values, final String criteria, final char first,
			final char upper) {
		if (values != null) {
			for (final var value : values) {
				if (contains(value, criteria, first, upper)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Indicates the value contains the criterion, ignoring the case.
	 *
	 * @param value    The value, may be <code>null</code>.
	 * @param criteria The criterion, not empty.
	 * @param first    The folded first char of the criterion.
	 * @param upper    The upper case of the folded first char: an ASCII char of the value is compared to both cases
	 *                 without folding it, only an ASCII char folds to an ASCII char.
	 * @return <code>true</code> when the value contains the criterion.
	 */
	private static boolean contains(final String value, final String criteria, final char first, final char upper) {
		if (value == null) {
			return false;
		}
		final var others = criteria.length() - 1;
		final var end = value.length() - others;
		for (var i = 0; i < end; i++) {
			final var c = value.charAt(i);
			if ((c == first || c == upper || c >= 128 && fold(c) == first)
					&& value.regionMatches(true, i + 1, criteria, 1, others)) {
				return true;
			}
		}
		return false;
	}
}
