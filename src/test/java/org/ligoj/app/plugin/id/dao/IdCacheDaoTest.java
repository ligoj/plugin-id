/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.*;
import org.ligoj.app.model.*;
import org.ligoj.bootstrap.AbstractJpaTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

/**
 * Test class of {@link IdCacheDao}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:/META-INF/spring/application-context-test.xml"})
@Rollback
@Transactional
class IdCacheDaoTest extends AbstractJpaTest {

	@Autowired
	private IdCacheDao dao;

	@Autowired
	private DelegateOrgRepository delegateOrgRepository;

	@Test
	void addGroupToGroup() {
		dao.create(new GroupOrg("dng3", "Name-SG-other", null), Collections.emptyMap());
		Assertions.assertEquals(0, em.createQuery("FROM CacheMembership WHERE group.id = :id AND subGroup.id = :sid")
				.setParameter("id", "group").setParameter("sid", "name-sg-other").getResultList().size());
		dao.addGroupToGroup(new GroupOrg("dng3", "Name-SG-other", null), new GroupOrg("dng", "Group", null));

		var memberships = em
				.createQuery("FROM CacheMembership WHERE group.id = :id AND subGroup.id = :sid", CacheMembership.class)
				.setParameter("id", "group").setParameter("sid", "name-sg-other").getResultList();
		Assertions.assertEquals(1, memberships.size());
		Assertions.assertEquals("group", memberships.getFirst().getGroup().getId());
		Assertions.assertEquals("name-sg-other", memberships.getFirst().getSubGroup().getId());
		Assertions.assertNull(memberships.getFirst().getUser());
	}

	@Test
	void addUserToGroup() {
		em.createQuery("DELETE FROM CacheMembership").executeUpdate();
		Assertions.assertEquals(0, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
		dao.addUserToGroup(newUser("u0"), new GroupOrg("dng", "Group", null));
		Assertions.assertEquals(1, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
	}

	private void checkUser() {
		final CacheUser user3 = em.find(CacheUser.class, "u");
		Assertions.assertNotNull(user3);
		Assertions.assertEquals("u", user3.getId());
		Assertions.assertEquals("company", user3.getCompany().getId());
		Assertions.assertEquals("f", user3.getFirstName());
		Assertions.assertEquals("l", user3.getLastName());
		Assertions.assertEquals("mail", user3.getMails());
	}

	@Test
	void createGroup() {
		Assertions.assertEquals(0, em.createQuery("FROM CacheGroup WHERE id = :id").setParameter("id", "name-sg-other")
				.getResultList().size());
		dao.create(new GroupOrg("dng3", "Name-SG-other", null), Collections.emptyMap());
		var group = em.find(CacheGroup.class, "name-sg-other");
		Assertions.assertNotNull(group);
		Assertions.assertEquals("name-sg-other", group.getId());
		Assertions.assertEquals("Name-SG-other", group.getName());
		Assertions.assertEquals("dng3", group.getDescription());
	}

	@Test
	void createCompany() {
		Assertions.assertEquals(0, em.createQuery("FROM CacheCompany WHERE id = :id").setParameter("id", "new-company")
				.getResultList().size());
		dao.create(new CompanyOrg("New Company", "new-company"));
		final var company = em.find(CacheCompany.class, "new-company");
		Assertions.assertNotNull(company);
		Assertions.assertEquals("new-company", company.getId());
		Assertions.assertEquals("new-company", company.getName());
		Assertions.assertEquals("New Company", company.getDescription());

		// Redundant creation
		dao.create(new CompanyOrg("New Company", "new-company"), Map.of("new-company", company));
	}

	@Test
	void createUser() {
		Assertions.assertEquals(1, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
		final var company = new CacheCompany();
		company.setId("company");
		company.setName("Company");
		company.setDescription("cn=company");
		em.persist(company);
		em.flush();
		em.clear();

		dao.create(newUser());

		Assertions.assertEquals("cn=company", em.find(CacheCompany.class, "company").getDescription());
		Assertions.assertNotNull(em.find(CacheCompany.class, "another-company"));
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		checkUser();
	}

	@Test
	void deleteCompany() {
		Assertions.assertEquals(2, em.createQuery("FROM CacheMembership WHERE group.id = :id")
				.setParameter("id", "group").getResultList().size());
		final var user = new UserOrg();
		user.setId("u0");
		dao.delete(user);
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		em.clear();
		final var company = new CompanyOrg("dna", "another-company");

		dao.delete(company);

		Assertions.assertNull(em.find(CacheCompany.class, "another-company"));
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		Assertions.assertNull(em.find(CacheUser.class, "u0"));
		Assertions.assertEquals(1, em.createQuery("FROM CacheMembership WHERE group.id = :id")
				.setParameter("id", "group").getResultList().size());
	}

	@Test
	void deleteCompanyNotEmpty() {
		Assertions.assertEquals(2, em.createQuery("FROM CacheMembership WHERE group.id = :id")
				.setParameter("id", "group").getResultList().size());
		Assertions.assertNotNull(em.find(CacheUser.class, "u0"));
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		em.clear();
		final var company = new CompanyOrg("dna", "another-company");
		Assertions.assertThrows(DataIntegrityViolationException.class, () -> dao.delete(company));
	}

	@Test
	void deleteGroup() {
		Assertions.assertEquals(2, em.createQuery("FROM CacheMembership WHERE group.id = :id")
				.setParameter("id", "group").getResultList().size());
		final var group = new GroupOrg("dng", "Group", null);

		dao.delete(group);

		Assertions.assertNotNull(em.find(CacheCompany.class, "another-company"));
		Assertions.assertNull(em.find(CacheGroup.class, "group"));
		Assertions.assertNotNull(em.find(CacheUser.class, "u0"));
		Assertions.assertEquals(0, em.createQuery("FROM CacheMembership WHERE group.id = :id")
				.setParameter("id", "group").getResultList().size());
	}

	@Test
	void deleteUser() {
		Assertions.assertEquals(1, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
		final var user = new UserOrg();
		user.setId("u0");

		dao.delete(user);

		Assertions.assertNotNull(em.find(CacheCompany.class, "another-company"));
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		Assertions.assertNull(em.find(CacheUser.class, "u0"));
		Assertions.assertEquals(0, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
	}

	@Test
	void empty() {
		Assertions.assertEquals(2, em.createQuery("FROM CacheMembership WHERE group.id = :id")
				.setParameter("id", "group").getResultList().size());
		final var group = new GroupOrg("dng", "Group", null);
		dao.empty(group);
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		Assertions.assertEquals(0, em.createQuery("FROM CacheMembership WHERE group.id = :id")
				.setParameter("id", "group").getResultList().size());
	}

	@BeforeEach
	void initDbCache() {
		final var company = new CacheCompany();
		company.setId("another-company");
		company.setName("Another-Company");
		company.setDescription("dna"); // DN
		em.persist(company);
		final var group = new CacheGroup();
		group.setId("group");
		group.setName("Group");
		group.setDescription("dng"); // DN
		em.persist(group);

		final var deletedGroup = new CacheGroup();
		deletedGroup.setId("old-group");
		deletedGroup.setName("OldGroup");
		deletedGroup.setDescription("old-dng"); // DN
		em.persist(deletedGroup);

		final var subgroup = new CacheGroup();
		subgroup.setId("another-group");
		subgroup.setName("Another-Group");
		subgroup.setDescription("dng2"); // DN
		em.persist(subgroup);
		final var user = new CacheUser();
		user.setId("u0");
		user.setCompany(company);
		em.persist(user);
		final var membership = new CacheMembership();
		membership.setGroup(group);
		membership.setUser(user);
		em.persist(membership);

		final var membershipSubGroup = new CacheMembership();
		membershipSubGroup.setGroup(group);
		membershipSubGroup.setSubGroup(subgroup);
		em.persist(membershipSubGroup);

		// Project group
		final var project = new Project();
		project.setPkey("pj");
		project.setName("Project");
		project.setTeamLeader("u0");
		em.persist(project);

		final var node = new Node();
		node.setId("service:id");
		node.setName("ID");
		em.persist(node);

		// Valid subscription
		var subscription = new Subscription();
		subscription.setNode(node);
		subscription.setProject(project);
		em.persist(subscription);

		final var parameter = new Parameter();
		parameter.setOwner(node);
		parameter.setId("service:id:group");
		em.persist(parameter);

		var value = new ParameterValue();
		value.setParameter(parameter);
		value.setData("group");
		value.setSubscription(subscription);
		em.persist(value);

		// Subscription related to a deleted group, with "CacheProjectGroup" and already linked
		subscription = new Subscription();
		subscription.setNode(node);
		subscription.setProject(project);
		em.persist(subscription);
		value = new ParameterValue();
		value.setParameter(parameter);
		value.setData("group2");
		value.setSubscription(subscription);
		em.persist(value);

		// Subscription related to a deleted group
		subscription = new Subscription();
		subscription.setNode(node);
		subscription.setProject(project);
		em.persist(subscription);

		value = new ParameterValue();
		value.setParameter(parameter);
		value.setData("deleted-group");
		value.setSubscription(subscription);
		em.persist(value);

		// Subscription related to another deleted group, without "CacheProjectGroup"
		final var projectUnlinked = new Project();
		projectUnlinked.setPkey("pj2");
		projectUnlinked.setName("Project2");
		projectUnlinked.setTeamLeader("u0");
		em.persist(projectUnlinked);
		subscription = new Subscription();
		subscription.setNode(node);
		subscription.setProject(projectUnlinked);
		em.persist(subscription);

		value = new ParameterValue();
		value.setParameter(parameter);
		value.setData("deleted-group2");
		value.setSubscription(subscription);
		em.persist(value);

		em.flush();
		em.clear();
	}

	private UserOrg newUser() {
		final var user = new UserOrg();
		user.setId("u");
		user.setFirstName("f");
		user.setLastName("l");
		user.setCompany("company");
		user.setGroups(Collections.singleton("group"));
		user.setMails(Collections.singletonList("mail"));
		return user;
	}

	private UserOrg newUser(final String login) {
		final var user = new UserOrg();
		user.setId(login);
		return user;
	}

	@Test
	void removeGroupFromGroup() {
		Assertions.assertEquals(1, em.createQuery("FROM CacheMembership WHERE group.id = :id AND subGroup.id = :sid")
				.setParameter("id", "group").setParameter("sid", "another-group").getResultList().size());
		dao.removeGroupFromGroup(new GroupOrg("dng2", "Another-Group", null), new GroupOrg("dng", "Group", null));
		Assertions.assertEquals(0, em.createQuery("FROM CacheMembership WHERE group.id = :id AND subGroup.id = :sid")
				.setParameter("id", "group").setParameter("sid", "another-group").getResultList().size());
	}

	@Test
	void removeUserFromGroup() {
		Assertions.assertEquals(1, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
		dao.removeUserFromGroup(newUser("u0"), new GroupOrg("dng", "Group", null));
		Assertions.assertEquals(0, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
	}

	@Test
	void reset() {
		final var companies = new HashMap<String, CompanyOrg>();
		companies.put("company", new CompanyOrg("dn=company1", "Company"));
		final var groups = new HashMap<String, GroupOrg>();
		final var members = new HashSet<String>();
		members.add("u");
		final var group = new GroupOrg("dn=group1", "Group", members);
		final var group2 = new GroupOrg("dn=group2", "Group2", members);
		final var group3 = new GroupOrg("dn=group3", "Group3", members);
		group.setSubGroups(Set.of("group"));
		groups.put("group", group);
		groups.put("group2", group2);
		groups.put("group3", group3);
		final var user = newUser();
		final var user2 = new UserOrg();
		user2.setId("u2");
		user2.setFirstName("f");
		user2.setLastName("l");
		user2.setCompany("company");
		user2.setGroups(Collections.emptyList());
		final var users = new HashMap<String, UserOrg>();
		users.put("u", user);
		users.put("u2", user2);

		// Updated users
		final var cacheUser2 = new CacheUser();
		cacheUser2.setId("u2");
		cacheUser2.setCompany(em.find(CacheCompany.class, "company"));
		em.persist(cacheUser2);
		final var cacheMembership = new CacheMembership();
		cacheMembership.setGroup(em.find(CacheGroup.class, "old-group")); // Membership will be removed
		cacheMembership.setUser(cacheUser2);
		em.persist(cacheMembership);
		final var cacheGroup2 = new CacheGroup();
		cacheGroup2.setId("group2");
		cacheGroup2.setName("Group2");
		cacheGroup2.setDescription("dng2"); // DN
		em.persist(cacheGroup2);

		// No change for these memberships
		final var cacheMembership2 = new CacheMembership();
		cacheMembership2.setGroup(cacheGroup2);
		cacheMembership2.setUser(cacheUser2);
		em.persist(cacheMembership2);
		final var membershipSubGroup = new CacheMembership();
		membershipSubGroup.setGroup(em.find(CacheGroup.class, "group"));
		membershipSubGroup.setSubGroup(cacheGroup2);
		em.persist(membershipSubGroup);

		final var deletedCompany = new CacheCompany();
		deletedCompany.setId("deleted-company");
		deletedCompany.setDescription("deleted-company");
		deletedCompany.setName("deleted-company");
		em.persist(deletedCompany);
		final var deletedGroup = new CacheGroup();
		deletedGroup.setId("deleted-group");
		deletedGroup.setDescription("deleted-group");
		deletedGroup.setName("deleted-group");
		em.persist(deletedGroup);
		final var deletedUser = new CacheUser();
		deletedUser.setId("deleted-user");
		deletedUser.setCompany(deletedCompany);
		em.persist(deletedUser);
		final var deletedMembershipFromDeletedUser = new CacheMembership();
		deletedMembershipFromDeletedUser.setGroup(deletedGroup);
		deletedMembershipFromDeletedUser.setUser(deletedUser);
		em.persist(deletedMembershipFromDeletedUser);


		// Unlinked memberships
		final var cacheGroup3 = new CacheGroup();
		cacheGroup3.setId("group3");
		cacheGroup3.setDescription("Group3");
		cacheGroup3.setName("group3");
		em.persist(cacheGroup3);

		final var unlinkedMembershipSubGroup = new CacheMembership();
		unlinkedMembershipSubGroup.setGroup(em.find(CacheGroup.class, "group"));
		unlinkedMembershipSubGroup.setSubGroup(cacheGroup3);
		em.persist(unlinkedMembershipSubGroup);

		final var unlinkedMembership = new CacheMembership();
		unlinkedMembership.setGroup(deletedGroup);
		unlinkedMembership.setUser(deletedUser);
		em.persist(unlinkedMembership);

		// Add delegates related to some containers

		// Broken, non-existing project related group
		final var brokenGroup = new CacheGroup();
		brokenGroup.setId("broken-project-group");
		brokenGroup.setName("broken_project_group");
		brokenGroup.setDescription("cn=broken_project_group");
		em.persist(brokenGroup);

		final var project = new Project();
		project.setName("broken_project_group");
		project.setPkey("broken-project-group");
		em.persist(project);
		em.flush();

		final var unlinkedProjectGroup = new CacheProjectGroup();
		unlinkedProjectGroup.setGroup(cacheGroup2);
		unlinkedProjectGroup.setProject(em.createQuery("FROM Project WHERE pkey = :pkey", Project.class).setParameter("pkey", "pj").getSingleResult());
		em.persist(unlinkedProjectGroup);
		em.flush();

		final var brokenProjectGroup2 = new CacheProjectGroup();
		brokenProjectGroup2.setGroup(brokenGroup);
		brokenProjectGroup2.setProject(project);
		em.persist(brokenProjectGroup2);
		em.flush();

		// Broken, non-existing related group
		final var brokenDelegate = new DelegateOrg();
		brokenDelegate.setName("broken");
		brokenDelegate.setDn("dn=group1");
		brokenDelegate.setType(DelegateType.GROUP);
		brokenDelegate.setReceiver("u2");
		brokenDelegate.setReceiverDn("uid=u2,dn=company1");
		brokenDelegate.setReceiverType(ReceiverType.USER);
		delegateOrgRepository.saveAndFlush(brokenDelegate);

		// Valid but DN need update
		final var updateDelegate = new DelegateOrg();
		updateDelegate.setName("company");
		updateDelegate.setDn("dn=old-company");
		updateDelegate.setType(DelegateType.COMPANY);
		updateDelegate.setReceiver("group");
		updateDelegate.setReceiverDn("dn=old-group");
		updateDelegate.setReceiverType(ReceiverType.GROUP);
		delegateOrgRepository.saveAndFlush(updateDelegate);

		// Valid but DN and up-to-date
		final var up2dateDelegate = new DelegateOrg();
		up2dateDelegate.setName("company");
		up2dateDelegate.setDn("dn=company1");
		up2dateDelegate.setType(DelegateType.COMPANY);
		up2dateDelegate.setReceiver("group");
		up2dateDelegate.setReceiverDn("dn=group1");
		up2dateDelegate.setReceiverType(ReceiverType.GROUP);
		delegateOrgRepository.saveAndFlush(up2dateDelegate);
		em.flush();

		// Pre state
		Assertions.assertNotNull(em.find(CacheCompany.class, "another-company"));
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		Assertions.assertNotNull(em.find(CacheUser.class, "u0"));
		Assertions.assertEquals(3, delegateOrgRepository.count());

		dao.reset(companies, groups, users);

		// Check previous cache is deleted
		Assertions.assertNull(em.find(CacheCompany.class, "another-company"));
		Assertions.assertNull(em.find(CacheGroup.class, "another-group"));
		Assertions.assertNull(em.find(CacheUser.class, "u0"));

		// Check the new state
		final var company = em.find(CacheCompany.class, "company");
		Assertions.assertNotNull(company);
		Assertions.assertEquals("company", company.getId());
		Assertions.assertEquals("Company", company.getName());
		Assertions.assertEquals("dn=company1", company.getDescription());

		final var groupFromEm = em.find(CacheGroup.class, "group");
		Assertions.assertNotNull(groupFromEm);
		Assertions.assertEquals("group", groupFromEm.getId());
		Assertions.assertEquals("Group", groupFromEm.getName());
		Assertions.assertEquals("dn=group1", groupFromEm.getDescription());
		checkUser();
		final var memberships = em.createQuery("FROM CacheMembership", CacheMembership.class)
				.getResultList();
		Assertions.assertEquals(2, memberships.size());
		Assertions.assertEquals("group", memberships.getFirst().getGroup().getId());
		Assertions.assertNull(memberships.getFirst().getSubGroup());
		Assertions.assertEquals("u", memberships.getFirst().getUser().getId());

		// Check the state of the previous delegates
		Assertions.assertEquals(2, delegateOrgRepository.count());

		// Broken and deleted delegate
		Assertions.assertFalse(delegateOrgRepository.existsById(brokenDelegate.getId()));

		// Updated delegate
		Assertions.assertEquals("dn=company1", delegateOrgRepository.findOneExpected(updateDelegate.getId()).getDn());
		Assertions.assertEquals("dn=group1",
				delegateOrgRepository.findOneExpected(updateDelegate.getId()).getReceiverDn());

		// Up-to-date delegate
		Assertions.assertEquals("dn=company1", delegateOrgRepository.findOneExpected(up2dateDelegate.getId()).getDn());
		Assertions.assertEquals("dn=group1",
				delegateOrgRepository.findOneExpected(up2dateDelegate.getId()).getReceiverDn());

		// Check the project groups
		final var pGroups = em.createQuery("FROM CacheProjectGroup", CacheProjectGroup.class)
				.getResultList();
		Assertions.assertEquals(2, pGroups.size()); // Group & Group2

		// Redundant reset
		dao.reset(companies, groups, users);
	}

	@Test
	void updateUser() {
		Assertions.assertEquals(1, em.createQuery("FROM CacheMembership WHERE user.id = :id").setParameter("id", "u0")
				.getResultList().size());
		final var newUser = newUser();
		newUser.setId("u0");
		newUser.setFirstName("F");
		newUser.setLastName("L");
		newUser.setCompany("another-company");
		newUser.setMails(null);
		dao.update(newUser);

		Assertions.assertNotNull(em.find(CacheCompany.class, "another-company"));
		Assertions.assertNotNull(em.find(CacheGroup.class, "group"));
		final var user3 = em.find(CacheUser.class, "u0");
		Assertions.assertNotNull(user3);
		Assertions.assertEquals("u0", user3.getId());
		Assertions.assertEquals("another-company", user3.getCompany().getId());
		Assertions.assertEquals("F", user3.getFirstName());
		Assertions.assertEquals("L", user3.getLastName());
		Assertions.assertNull(user3.getMails());
		final var memberships = em
				.createQuery("FROM CacheMembership WHERE user.id = :id", CacheMembership.class).setParameter("id", "u0")
				.getResultList();
		Assertions.assertEquals(1, memberships.size());
		Assertions.assertEquals("group", memberships.getFirst().getGroup().getId());
		Assertions.assertNull(memberships.getFirst().getSubGroup());
		Assertions.assertEquals("u0", memberships.getFirst().getUser().getId());
	}
}
