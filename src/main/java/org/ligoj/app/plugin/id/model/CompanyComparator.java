package org.ligoj.app.plugin.id.model;

import org.ligoj.app.api.UserLdap;

/**
 * Order by company.
 */
public class CompanyComparator extends AbstractNameComparator {

	@Override
	public int compare(final UserLdap o1, final UserLdap o2) {
		return compare(o1, o2, UserLdap::getCompany);
	}

}
