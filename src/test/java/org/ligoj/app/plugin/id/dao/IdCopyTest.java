/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import jakarta.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.ResourceOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.plugin.id.dao.AbstractMemCacheRepository.CacheDataType;
import org.ligoj.bootstrap.AbstractJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link IdCopy}: the identity data rebuilt from the shared cache tables, as the full read built it.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class IdCopyTest extends AbstractJpaTest {

	private static final Instant NOW = Instant.parse("2026-10-24T08:00:00Z");

	private static final Duration DAY = Duration.ofDays(1);

	/**
	 * A daily full read.
	 */
	private static final UnaryOperator<Instant> DAILY = last -> last.plus(DAY);

	private static final String NODE = "service:id:ldap:dig";

	private static final String FINGERPRINT = "fingerprint";

	/**
	 * The companies of a provider without specific finishing.
	 */
	private static final Function<Collection<CacheCompany>, Map<String, CompanyOrg>> COMPANIES = rows -> rows.stream()
			.collect(Collectors.toMap(CacheCompany::getId, r -> new CompanyOrg(r.getDescription(), r.getName())));

	@Autowired
	private IdCacheDao dao;

	@Autowired
	private IdCopy copy;

	@Autowired
	private CacheCopyStateRepository stateRepository;

	private UserOrg newUser(final String id, final String... groups) {
		final var user = new UserOrg();
		user.setId(id);
		user.setFirstName("F-" + id);
		user.setLastName("L-" + id);
		user.setCompany("company");
		user.setMails(new ArrayList<>(List.of(id + "@sample.com")));
		user.setGroups(new ArrayList<>(List.of(groups)));
		user.setCustomAttributes(new HashMap<>());
		return user;
	}

	/**
	 * A full read of two companies, three groups (a sub-group) and two users, written by the reset, then the state.
	 */
	private Map<String, UserOrg> fullRead(final UserOrg... more) {
		final var companies = Map.of("company", new CompanyOrg("ou=company,dc=sample", "Company"), "other",
				new CompanyOrg("ou=other,dc=sample", "Other"));
		final var group = new GroupOrg("cn=group,dc=sample", "Group", new HashSet<>(Set.of("fdaugan", "jdoe")));
		final var sub = new GroupOrg("cn=sub,dc=sample", "Sub", new HashSet<>(Set.of("jdoe")));
		final var other = new GroupOrg("cn=other,dc=sample", "Other", new HashSet<>());
		group.getSubGroups().add("sub");
		sub.setParent("group");
		final var groups = Map.of("group", group, "sub", sub, "other", other);
		final var users = new HashMap<String, UserOrg>();
		final var fdaugan = newUser("fdaugan", "group");
		fdaugan.setDn("uid=fdaugan,ou=company,dc=sample");
		fdaugan.setMails(new ArrayList<>(List.of("fdaugan@sample.com", "fabrice@other.com")));
		fdaugan.setSecured(true);
		fdaugan.setLocked(NOW.minusSeconds(3600));
		fdaugan.setLockedBy("admin");
		fdaugan.setIsolated("ligoj");
		fdaugan.setDepartment("3890");
		fdaugan.setLocalId("8234");
		fdaugan.setCustomAttributes(new HashMap<>(Map.of("employeeType", "B-7", "title", "Dev")));
		users.put("fdaugan", fdaugan);
		users.put("jdoe", newUser("jdoe", "group", "sub"));
		for (final var user : more) {
			users.put(user.getId(), user);
		}
		dao.reset(companies, groups, users);
		copy.saveState(NODE, FINGERPRINT, NOW);
		em.flush();
		em.clear();
		return users;
	}

	private Map<CacheDataType, Map<String, ? extends ResourceOrg>> load() {
		return copy.load(NODE, FINGERPRINT, NOW.plusSeconds(60), DAILY, COMPANIES).orElseThrow().data();
	}

	@SuppressWarnings("unchecked")
	private static <T extends ResourceOrg> Map<String, T> of(final Map<CacheDataType, Map<String, ? extends ResourceOrg>> data,
			final CacheDataType type) {
		return (Map<String, T>) data.get(type);
	}

	@Test
	void loadRebuildsTheData() {
		final var users = fullRead();
		final var snapshot = copy.load(NODE, FINGERPRINT, NOW.plusSeconds(60), DAILY, COMPANIES).orElseThrow();
		Assertions.assertEquals(NOW, snapshot.lastFullRead());

		final Map<String, UserOrg> loaded = of(snapshot.data(), CacheDataType.USER);
		Assertions.assertEquals(users.keySet(), loaded.keySet());
		final var fdaugan = loaded.get("fdaugan");
		Assertions.assertEquals("uid=fdaugan,ou=company,dc=sample", fdaugan.getDn());
		Assertions.assertEquals("F-fdaugan", fdaugan.getFirstName());
		Assertions.assertEquals("L-fdaugan", fdaugan.getLastName());
		Assertions.assertEquals("company", fdaugan.getCompany());
		Assertions.assertEquals(List.of("fdaugan@sample.com", "fabrice@other.com"), fdaugan.getMails());
		Assertions.assertTrue(fdaugan.isSecured());
		Assertions.assertEquals(NOW.minusSeconds(3600), fdaugan.getLocked());
		Assertions.assertEquals("admin", fdaugan.getLockedBy());
		Assertions.assertEquals("ligoj", fdaugan.getIsolated());
		Assertions.assertEquals("3890", fdaugan.getDepartment());
		Assertions.assertEquals("8234", fdaugan.getLocalId());
		Assertions.assertEquals(Map.of("employeeType", "B-7", "title", "Dev"), fdaugan.getCustomAttributes());
		Assertions.assertEquals(Set.of("group"), new HashSet<>(fdaugan.getGroups()));
		Assertions.assertEquals(Set.of("group", "sub"), new HashSet<>(loaded.get("jdoe").getGroups()));

		final Map<String, GroupOrg> groups = of(snapshot.data(), CacheDataType.GROUP);
		Assertions.assertEquals(Set.of("group", "sub", "other"), groups.keySet());
		Assertions.assertEquals("cn=group,dc=sample", groups.get("group").getDn());
		Assertions.assertEquals("Group", groups.get("group").getName());
		Assertions.assertEquals(Set.of("fdaugan", "jdoe"), groups.get("group").getMembers());
		Assertions.assertEquals(Set.of("sub"), groups.get("group").getSubGroups());
		Assertions.assertEquals("group", groups.get("sub").getParent());
		Assertions.assertNull(groups.get("group").getParent());

		final Map<String, CompanyOrg> companies = of(snapshot.data(), CacheDataType.COMPANY);
		Assertions.assertEquals(Set.of("company", "other"), companies.keySet());
		Assertions.assertEquals("ou=company,dc=sample", companies.get("company").getDn());
		Assertions.assertEquals("Company", companies.get("company").getName());
	}

	@Test
	void loadKeepsBareUser() {
		final var bare = new UserOrg();
		bare.setId("bare");
		bare.setGroups(new ArrayList<>());
		fullRead(bare);
		final var loaded = (UserOrg) load().get(CacheDataType.USER).get("bare");
		Assertions.assertEquals(List.of(), loaded.getMails());
		Assertions.assertFalse(loaded.isSecured());
		Assertions.assertNull(loaded.getLocked());
		Assertions.assertNull(loaded.getLockedBy());
		Assertions.assertNull(loaded.getIsolated());
		Assertions.assertNull(loaded.getDepartment());
		Assertions.assertNull(loaded.getLocalId());
		Assertions.assertNull(loaded.getCompany());
		Assertions.assertNull(loaded.getDn());
		Assertions.assertEquals(Map.of(), loaded.getCustomAttributes());
	}

	/**
	 * Long values of a real directory: many mail aliases, a long custom attribute value.
	 */
	@Test
	void loadKeepsLongValues() {
		final var mails = new ArrayList<String>();
		for (var i = 0; i < 60; i++) {
			mails.add("functional.account.alias-" + i + "@sample.com");
		}
		final var functional = newUser("functional");
		functional.setMails(mails);
		functional.setCustomAttributes(new HashMap<>(Map.of("description", "d".repeat(5000))));
		fullRead(functional);
		final var loaded = (UserOrg) load().get(CacheDataType.USER).get("functional");
		Assertions.assertEquals(mails, loaded.getMails());
		Assertions.assertEquals(5000, loaded.getCustomAttributes().get("description").length());
	}

	@Test
	void loadRefusesNoState() {
		fullRead();
		stateRepository.deleteAll();
		Assertions.assertTrue(copy.load(NODE, FINGERPRINT, NOW, DAILY, COMPANIES).isEmpty());
	}

	@Test
	void loadRefusesOtherNode() {
		fullRead();
		Assertions.assertTrue(copy.load("service:id:ldap:other", FINGERPRINT, NOW, DAILY, COMPANIES).isEmpty());
	}

	@Test
	void loadRefusesOtherFingerprint() {
		fullRead();
		Assertions.assertTrue(copy.load(NODE, "other", NOW, DAILY, COMPANIES).isEmpty());
	}

	@Test
	void loadRefusesExpired() {
		fullRead();
		Assertions.assertTrue(copy.load(NODE, FINGERPRINT, NOW.plus(DAY).minusSeconds(1), DAILY, COMPANIES).isPresent());
		Assertions.assertTrue(copy.load(NODE, FINGERPRINT, NOW.plus(DAY), DAILY, COMPANIES).isEmpty());
	}

	/**
	 * An eternal cache (`cache.*.ttl=-1`): a copy never expires.
	 */
	@Test
	void loadNeverExpiresWithEternalCache() {
		fullRead();
		Assertions.assertTrue(copy.load(NODE, FINGERPRINT, NOW.plus(Duration.ofDays(3650)), _ -> Instant.MAX, COMPANIES).isPresent());
	}

	/**
	 * A cache expiring at fixed times: the copy expires at the first time after its full read.
	 */
	@Test
	void loadExpiresAtTheNextTime() {
		fullRead();
		final UnaryOperator<Instant> nightly = _ -> NOW.plus(Duration.ofHours(19));
		Assertions.assertTrue(copy.load(NODE, FINGERPRINT, NOW.plus(Duration.ofHours(19)).minusSeconds(1), nightly, COMPANIES).isPresent());
		Assertions.assertTrue(copy.load(NODE, FINGERPRINT, NOW.plus(Duration.ofHours(19)), nightly, COMPANIES).isEmpty());
	}

	/**
	 * The shared tables hold the data of one node: the state of any other node is forgotten.
	 */
	@Test
	void saveStateForgetsTheOtherNodes() {
		fullRead();
		copy.saveState("service:id:ldap:other", FINGERPRINT, NOW);
		Assertions.assertTrue(stateRepository.findById(NODE).isEmpty());
		copy.saveState("service:id:ldap:other", "again", NOW.plusSeconds(1));
		Assertions.assertEquals(1, stateRepository.count());
		Assertions.assertEquals("again", stateRepository.findById("service:id:ldap:other").orElseThrow().getFingerprint());
	}
}
