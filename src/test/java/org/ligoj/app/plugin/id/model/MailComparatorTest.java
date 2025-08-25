/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import java.util.ArrayList;
import java.util.Collections;

import org.apache.commons.lang3.ObjectUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.UserOrg;

/**
 * Test class of {@link MailComparator}
 */
class MailComparatorTest {

	private UserOrg newSimpleUser(final String firstName, final String login) {
		final UserOrg simpleUser = new UserOrg();
		simpleUser.setMails(firstName == null ? new ArrayList<>() : Collections.singletonList(firstName));
		simpleUser.setName(ObjectUtils.getIfNull(login, "l"));
		return simpleUser;
	}

	@Test
	void compareNull() {
		Assertions.assertEquals(0, new MailComparator().compare(newSimpleUser(null, null), newSimpleUser(null, null)));
	}

	@Test
	void compareNull0() {
		final UserOrg o1 = newSimpleUser("a", null);
		Assertions.assertEquals(1, new MailComparator().compare(o1, newSimpleUser(null, null)));
	}

	@Test
	void compareNull1() {
		final UserOrg o2 = newSimpleUser("a", null);
		Assertions.assertEquals(-1, new MailComparator().compare(newSimpleUser(null, null), o2));
	}

	@Test
	void compare() {
		final UserOrg o1 = newSimpleUser("a", null);
		final UserOrg o2 = newSimpleUser("c", null);
		Assertions.assertEquals(-2, new MailComparator().compare(o1, o2));
	}

}
