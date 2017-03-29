package org.ligoj.app.plugin.id.model;

import java.util.Comparator;

import org.ligoj.app.iam.UserOrg;

/**
 * Order by UID.
 */
public class LoginComparator implements Comparator<UserOrg> {

	@Override
	public int compare(final UserOrg o1, final UserOrg o2) {
		return o1.compareTo(o2);
	}

}
