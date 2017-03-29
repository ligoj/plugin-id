package org.ligoj.app.plugin.id.model;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.plugin.id.model.MailComparator;

/**
 * Test class of {@link MailComparator}
 */
public class MailComparatorTest {

	private UserOrg newSimpleUser(final String firstName, final String login) {
		final UserOrg simpleUser = new UserOrg();
		simpleUser.setMails(firstName == null ? new ArrayList<>() : Collections.singletonList(firstName));
		simpleUser.setName(ObjectUtils.defaultIfNull(login, "l"));
		return simpleUser;
	}

	@Test
	public void compareNull() {
		Assert.assertEquals(0, new MailComparator().compare(newSimpleUser(null, null), newSimpleUser(null, null)));
	}

	@Test
	public void compareNull0() {
		final UserOrg o1 = newSimpleUser("a", null);
		Assert.assertEquals(1, new MailComparator().compare(o1, newSimpleUser(null, null)));
	}

	@Test
	public void compareNull1() {
		final UserOrg o2 = newSimpleUser("a", null);
		Assert.assertEquals(-1, new MailComparator().compare(newSimpleUser(null, null), o2));
	}

	@Test
	public void compare() {
		final UserOrg o1 = newSimpleUser("a", null);
		final UserOrg o2 = newSimpleUser("c", null);
		Assert.assertEquals(-2, new MailComparator().compare(o1, o2));
	}

}
