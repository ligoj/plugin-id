package org.ligoj.app.plugin.id.model;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.api.UserLdap;
import org.ligoj.app.plugin.id.model.CompanyComparator;

/**
 * Test class of {@link CompanyComparator}
 */
public class CompanyComparatorTest {

	private UserLdap newSimpleUser(final String company, final String login) {
		final UserLdap simpleUser = new UserLdap();
		simpleUser.setCompany(company);
		simpleUser.setName(ObjectUtils.defaultIfNull(login, "l"));
		return simpleUser;
	}

	@Test
	public void compareNull() {
		Assert.assertEquals(0, new CompanyComparator().compare(newSimpleUser(null, null), newSimpleUser(null, null)));
	}

	@Test
	public void compareNull0() {
		final UserLdap o1 = newSimpleUser("a", null);
		Assert.assertEquals(1, new CompanyComparator().compare(o1, newSimpleUser(null, null)));
	}

	@Test
	public void compareNull1() {
		final UserLdap o2 = newSimpleUser("a", null);
		Assert.assertEquals(-1, new CompanyComparator().compare(newSimpleUser(null, null), o2));
	}

	@Test
	public void compare() {
		final UserLdap o1 = newSimpleUser("a", null);
		final UserLdap o2 = newSimpleUser("c", null);
		Assert.assertEquals(-2, new CompanyComparator().compare(o1, o2));
	}
}
