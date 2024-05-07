/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import org.ligoj.app.iam.UserOrg;

/**
 * Order by mail.
 */
public class MailComparator extends AbstractNameComparator {

	@Override
	public int compare(final UserOrg o1, final UserOrg o2) {
		return compare(o1, o2, this::toSafeString);
	}

	/**
	 * Return a safe string representation of the mail of a user.
	 */
	private String toSafeString(final UserOrg o1) {
		return o1.getMails().isEmpty() ? "" : o1.getMails().getFirst();
	}

}
