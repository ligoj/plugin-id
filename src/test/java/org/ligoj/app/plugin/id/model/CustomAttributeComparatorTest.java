/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.UserOrg;

/**
 * Test class of {@link CustomAttributeComparator}
 */
class CustomAttributeComparatorTest {

	private final CustomAttributeComparator comparator = new CustomAttributeComparator("badge");

	private UserOrg newUser(final String id, final String badge) {
		final var user = new UserOrg();
		user.setId(id);
		if (badge != null) {
			user.setCustomAttributes(Map.of("badge", badge));
		}
		return user;
	}

	@Test
	void compareByAttribute() {
		Assertions.assertTrue(comparator.compare(newUser("z-user", "A1"), newUser("a-user", "B2")) < 0);
		Assertions.assertTrue(comparator.compare(newUser("a-user", "B2"), newUser("z-user", "A1")) > 0);
	}

	@Test
	void compareFallbackToId() {
		// Equal attribute values → identifier decides
		Assertions.assertTrue(comparator.compare(newUser("a-user", "same"), newUser("b-user", "same")) < 0);
		// Absent custom attributes map → empty value, then identifier decides
		Assertions.assertTrue(comparator.compare(newUser("a-user", null), newUser("b-user", null)) < 0);
	}

	@Test
	void compareAbsentBeforePresent() {
		Assertions.assertTrue(comparator.compare(newUser("z-user", null), newUser("a-user", "A1")) < 0);
	}
}
