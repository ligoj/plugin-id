/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.UserOrg;

/**
 * Test class of {@link UserCriteria}
 */
class UserCriteriaTest {

	private UserOrg user() {
		final var user = new UserOrg();
		user.setId("ragnar");
		user.setFirstName("Ragnar");
		user.setLastName("Ahmed");
		user.setMails(List.of("ragnar.ahmed@sample.com"));
		user.setCustomAttributes(Map.of("uidFonctionnel", "UID Ragnar", "badge", "B-7"));
		return user;
	}

	@Test
	void matchesStandardAttributes() {
		Assertions.assertTrue(UserCriteria.matches(user(), null));
		Assertions.assertTrue(UserCriteria.matches(user(), " "));
		Assertions.assertTrue(UserCriteria.matches(user(), "RAGN"));
		Assertions.assertTrue(UserCriteria.matches(user(), "ahmed"));
		Assertions.assertTrue(UserCriteria.matches(user(), "@sample"));
		Assertions.assertFalse(UserCriteria.matches(user(), "nobody"));
	}

	@Test
	void matchesCustomAttributes() {
		// The visual identifier (a custom attribute) is searchable, case-insensitively
		Assertions.assertTrue(UserCriteria.matches(user(), "uid ragnar"));
		Assertions.assertTrue(UserCriteria.matches(user(), "b-7"));
		Assertions.assertFalse(UserCriteria.matches(user(), "uidFonctionnel"));
	}

	@Test
	void matchesWithoutOptionalAttributes() {
		final var user = new UserOrg();
		user.setId("bare");
		Assertions.assertTrue(UserCriteria.matches(user, "bar"));
		Assertions.assertFalse(UserCriteria.matches(user, "x"));
	}
}
