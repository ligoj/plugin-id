package org.ligoj.app.plugin.id.model;

import org.ligoj.app.api.UserOrg;

/**
 * Order by Last name.
 */
public class LastNameComparator extends AbstractNameComparator {

	@Override
	public int compare(final UserOrg o1, final UserOrg o2) {
		return compare(o1, o2, UserOrg::getLastName);
	}

}
