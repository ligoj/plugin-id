package org.ligoj.app.plugin.id.model;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.plugin.id.model.CompanyComparator;

/**
 * Test class of {@link CompanyComparator}
 */
public class CompanyComparatorTest {

	private UserOrg newSimpleUser(final String company, final String login) {
		final UserOrg simpleUser = new UserOrg();
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
		final UserOrg o1 = newSimpleUser("a", null);
		Assert.assertEquals(1, new CompanyComparator().compare(o1, newSimpleUser(null, null)));
	}

	@Test
	public void compareNull1() {
		final UserOrg o2 = newSimpleUser("a", null);
		Assert.assertEquals(-1, new CompanyComparator().compare(newSimpleUser(null, null), o2));
	}

	@Test
	public void compare() {
		final UserOrg o1 = newSimpleUser("a", null);
		final UserOrg o2 = newSimpleUser("c", null);
		Assert.assertEquals(-2, new CompanyComparator().compare(o1, o2));
	}
}
