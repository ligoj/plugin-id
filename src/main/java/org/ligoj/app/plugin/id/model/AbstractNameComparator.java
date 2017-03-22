package org.ligoj.app.plugin.id.model;

import java.util.function.Function;

import org.apache.commons.lang3.ObjectUtils;
import org.ligoj.app.api.UserOrg;

/**
 * Comparator based on a string attribute.
 */
public abstract class AbstractNameComparator extends LoginComparator {

	/**
	 * Compare two users, first with a specific data, then with their identifier.
	 * 
	 * @param o1
	 *            the first user to be compared.
	 * @param o2
	 *            the second user to be compared.
	 * @param nameProvider
	 *            The data provider to order the users.
	 * @return a negative integer, zero, or a positive integer as the
	 *         first argument is less than, equal to, or greater than the
	 *         second.
	 */
	protected int compare(final UserOrg o1, final UserOrg o2, final Function<UserOrg, String> nameProvider) {
		final int compareTo = ObjectUtils.defaultIfNull(nameProvider.apply(o1), "")
				.compareToIgnoreCase(ObjectUtils.defaultIfNull(nameProvider.apply(o2), ""));
		if (compareTo == 0) {
			// Equals, so compare with the identifier
			return super.compare(o1, o2);
		}
		return compareTo;
	}

}
