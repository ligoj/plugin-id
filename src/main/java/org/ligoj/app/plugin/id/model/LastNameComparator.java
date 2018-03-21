/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import org.ligoj.app.iam.UserOrg;

/**
 * Order by Last name.
 */
public class LastNameComparator extends AbstractNameComparator {

	@Override
	public int compare(final UserOrg o1, final UserOrg o2) {
		return compare(o1, o2, UserOrg::getLastName);
	}

}
