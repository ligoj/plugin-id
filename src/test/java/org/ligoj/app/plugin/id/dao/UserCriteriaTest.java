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
	void matchesCompanyAndGroups() {
		final var user = user();
		user.setCompany("department3");
		user.setGroups(List.of("billing-ops", "dig rha"));
		Assertions.assertTrue(UserCriteria.matches(user, "DEPARTMENT3"));
		Assertions.assertTrue(UserCriteria.matches(user, "billing"));
		Assertions.assertTrue(UserCriteria.matches(user, "rha"));
		Assertions.assertFalse(UserCriteria.matches(user, "department4"));
	}

	private UserOrg user(final String id) {
		final var user = new UserOrg();
		user.setId(id);
		return user;
	}

	/**
	 * The case is ignored as {@link String#regionMatches(boolean, int, String, int, int)} does, beyond ASCII too.
	 */
	@Test
	void matchesIgnoringCase() {
		Assertions.assertTrue(UserCriteria.matches(user("Kelvin \u212A"), "k"));
		Assertions.assertTrue(UserCriteria.matches(user("ſmith"), "SMITH"));
		Assertions.assertTrue(UserCriteria.matches(user("İstanbul"), "i"));
		Assertions.assertTrue(UserCriteria.matches(user("ÉLOÏSE"), "éloïse"));
		Assertions.assertTrue(UserCriteria.matches(user("søren"), "SØR"));
		Assertions.assertFalse(UserCriteria.matches(user("Ølaf"), "o"));
		Assertions.assertFalse(UserCriteria.matches(user("Straße"), "STRASSE"));
	}

	@Test
	void matchesAnywhere() {
		// A first char matching, then the next one not: the scan goes on
		Assertions.assertTrue(UserCriteria.matches(user("aab"), "ab"));
		Assertions.assertTrue(UserCriteria.matches(user("ab"), "AB"));
		// Longer than the value
		Assertions.assertFalse(UserCriteria.matches(user("ab"), "abc"));
	}

	@Test
	void matchesWithEmptyAttributes() {
		final var user = user("bare");
		user.setMails(List.of());
		user.setGroups(List.of());
		user.setCustomAttributes(Map.of());
		Assertions.assertFalse(UserCriteria.matches(user, "x"));
	}

	@Test
	void matchesWithoutOptionalAttributes() {
		final var user = new UserOrg();
		user.setId("bare");
		Assertions.assertTrue(UserCriteria.matches(user, "bar"));
		Assertions.assertFalse(UserCriteria.matches(user, "x"));
		// The first name alone
		user.setFirstName("Lara");
		Assertions.assertTrue(UserCriteria.matches(user, "lar"));
	}
}
