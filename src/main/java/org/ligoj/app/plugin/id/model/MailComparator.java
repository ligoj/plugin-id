package org.ligoj.app.plugin.id.model;

import org.ligoj.app.api.UserLdap;

/**
 * Order by mail.
 */
public class MailComparator extends AbstractNameComparator {

	@Override
	public int compare(final UserLdap o1, final UserLdap o2) {
		return compare(o1, o2, this::toSafeString);
	}

	/**
	 * Return a safe string representation of the mail of a user.
	 */
	private String toSafeString(final UserLdap o1) {
		return o1.getMails().isEmpty() ? "" : o1.getMails().get(0);
	}

}
