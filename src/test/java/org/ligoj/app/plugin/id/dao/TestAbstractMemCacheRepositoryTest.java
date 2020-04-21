/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.plugin.id.dao.AbstractMemCacheRepository.CacheDataType;
import org.ligoj.bootstrap.AbstractDataGeneratorTest;
import org.ligoj.bootstrap.core.SpringUtils;
import org.mockito.Mockito;
import org.springframework.context.ApplicationContext;

/**
 * Test class of {@link AbstractMemCacheRepository}
 */
class TestAbstractMemCacheRepositoryTest extends AbstractDataGeneratorTest {
	private UserOrg user;
	private GroupOrg groupLdap;
	private GroupOrg groupLdap2;
	private Map<String, GroupOrg> groups;
	private Map<String, CompanyOrg> companies;
	private Map<String, UserOrg> users;
	private SampleIdMemCacheRepository repository;

	@BeforeEach
	void init() {
		final var companyRepository = Mockito.mock(ICompanyRepository.class);
		final var groupRepository = Mockito.mock(IGroupRepository.class);
		final var userRepository = Mockito.mock(IUserRepository.class);
		final var iamProvider = Mockito.mock(IamProvider.class);
		final var applicationContext = Mockito.mock(ApplicationContext.class);
		SpringUtils.setSharedApplicationContext(applicationContext);
		final var iamConfiguration = new IamConfiguration();
		iamConfiguration.setCompanyRepository(companyRepository);
		iamConfiguration.setGroupRepository(groupRepository);
		iamConfiguration.setUserRepository(userRepository);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(iamConfiguration);

		companies = new HashMap<>();
		companies.put("company", new CompanyOrg("dnc", "Company"));
		groups = new HashMap<>();
		final var members = new HashSet<String>();
		members.add("u");
		groupLdap = new GroupOrg("dn", "Group", members);
		groups.put("group", groupLdap);
		groupLdap2 = new GroupOrg("dn2", "Group2", new HashSet<>());
		groups.put("group2", groupLdap2);
		user = new UserOrg();
		user.setId("u");
		user.setFirstName("f");
		user.setLastName("l");
		user.setCompany("company");
		final var userGroups = new ArrayList<String>();
		userGroups.add("group");
		user.setGroups(userGroups);
		user.setMails(Collections.singletonList("mail"));
		groupLdap.getMembers().add("u");
		final var user2 = new UserOrg();
		user2.setId("u2");
		user2.setFirstName("f");
		user2.setLastName("l");
		user2.setCompany("company");
		user2.setGroups(new ArrayList<>());
		users = new HashMap<>();
		users.put("u", user);
		users.put("u2", user2);
		Mockito.when(companyRepository.findAllNoCache()).thenReturn(companies);
		Mockito.when(groupRepository.findAllNoCache()).thenReturn(groups);
		Mockito.when(userRepository.findAllNoCache(groups)).thenReturn(users);
		Mockito.when(companyRepository.findAll()).thenReturn(companies);
		Mockito.when(groupRepository.findAll()).thenReturn(groups);
		Mockito.when(userRepository.findAll()).thenReturn(users);

		repository = new SampleIdMemCacheRepository();
		repository.setIamProvider(new IamProvider[] { iamProvider });
		repository.setCache(Mockito.mock(IdCacheDao.class));
	}

	@Test
	void getLdapData() {

		// Only there for coverage
		CacheDataType.values();
		CacheDataType.valueOf(CacheDataType.COMPANY.name());

		final var ldapData = repository.getData();

		Assertions.assertEquals("Company", ((CompanyOrg) ldapData.get(CacheDataType.COMPANY).get("company")).getName());
		Assertions.assertEquals("dnc", ((CompanyOrg) ldapData.get(CacheDataType.COMPANY).get("company")).getDn());
		final var groupLdap = (GroupOrg) ldapData.get(CacheDataType.GROUP).get("group");
		Assertions.assertEquals("dn", groupLdap.getDn());
		Assertions.assertEquals("group", groupLdap.getId());
		Assertions.assertEquals("Group", groupLdap.getName());
		final var user = (UserOrg) ldapData.get(CacheDataType.USER).get("u");
		Assertions.assertEquals("u", user.getId());
		Assertions.assertEquals("f", user.getFirstName());
		Assertions.assertEquals("l", user.getLastName());
		Assertions.assertEquals("company", user.getCompany());
		final var user2 = (UserOrg) ldapData.get(CacheDataType.USER).get("u2");
		Assertions.assertEquals("u2", user2.getId());
		Assertions.assertEquals("f", user2.getFirstName());
		Assertions.assertEquals("l", user2.getLastName());
		Assertions.assertEquals("company", user2.getCompany());
	}

	@Test
	void addUserToGroup() {
		Assertions.assertEquals(1, user.getGroups().size());

		repository.addUserToGroup(user, groupLdap2);

		Assertions.assertEquals(2, user.getGroups().size());
		Assertions.assertTrue(user.getGroups().contains("group2"));
		Assertions.assertTrue(groups.get("group2").getMembers().contains("u"));
	}

	@Test
	void removeUserFromGroup() {
		Assertions.assertEquals(1, user.getGroups().size());

		repository.removeUserFromGroup(user, groupLdap);

		Assertions.assertEquals(0, user.getGroups().size());
		Assertions.assertTrue(groups.get("group").getMembers().isEmpty());
	}

	@Test
	void addGroupToGroup() {
		final var parent = groupLdap2;
		final var child = groupLdap;

		// Check the initial status
		Assertions.assertEquals(0, child.getSubGroups().size());
		Assertions.assertEquals(0, child.getGroups().size());
		Assertions.assertEquals(0, parent.getGroups().size());
		Assertions.assertEquals(0, parent.getSubGroups().size());

		repository.addGroupToGroup(child, parent);

		// Check the new status
		Assertions.assertEquals(1, child.getGroups().size());
		Assertions.assertEquals(0, child.getSubGroups().size());
		Assertions.assertEquals(0, parent.getGroups().size());
		Assertions.assertEquals(1, parent.getSubGroups().size());
		Assertions.assertTrue(parent.getSubGroups().contains("group"));
		Assertions.assertTrue(child.getGroups().contains("group2"));
	}

	@Test
	void removeGroupFromGroup() {
		final var parent = groupLdap2;
		final var child = groupLdap;
		parent.getSubGroups().add(child.getId());
		child.getGroups().add(parent.getId());

		// Check the initial status
		Assertions.assertEquals(1, child.getGroups().size());
		Assertions.assertEquals(0, child.getSubGroups().size());
		Assertions.assertEquals(0, parent.getGroups().size());
		Assertions.assertEquals(1, parent.getSubGroups().size());

		repository.removeGroupFromGroup(child, parent);

		// Check the new status
		Assertions.assertEquals(0, child.getGroups().size());
		Assertions.assertEquals(0, child.getSubGroups().size());
		Assertions.assertEquals(0, parent.getGroups().size());
		Assertions.assertEquals(0, parent.getSubGroups().size());
	}

	@Test
	void createGroup() {
		final var newGroupLdap = new GroupOrg("dn3", "G3", new HashSet<>());

		Assertions.assertEquals(newGroupLdap, repository.create(newGroupLdap));

		Mockito.verify(repository.cache).create(newGroupLdap);
		Assertions.assertEquals(newGroupLdap, groups.get("g3"));
	}

	@Test
	void createCompany() {
		final var newCompanyLdap = new CompanyOrg("dn3", "C3");

		Assertions.assertEquals(newCompanyLdap, repository.create(newCompanyLdap));

		Mockito.verify(repository.cache).create(newCompanyLdap);
		Assertions.assertEquals(newCompanyLdap, companies.get("c3"));
	}

	@Test
	void createUser() {
		final var newUser = new UserOrg();
		newUser.setId("u3");
		newUser.setFirstName("f");
		newUser.setLastName("l");
		newUser.setCompany("company");

		Assertions.assertEquals(newUser, repository.create(newUser));

		Mockito.verify(repository.cache).create(newUser);
		Assertions.assertTrue(user.getGroups().contains("group"));
		Assertions.assertSame(newUser, users.get("u3"));
	}

	@Test
	void updateUser() {
		user.setFirstName("L");

		repository.update(user);

		Mockito.verify(repository.cache).update(user);
		Assertions.assertSame("L", users.get("u").getFirstName());
	}

	@Test
	void deleteCompany() {
		Assertions.assertTrue(companies.containsKey("company"));
		Assertions.assertEquals("company", user.getCompany());

		repository.delete(companies.get("company"));

		Assertions.assertFalse(companies.containsKey("company"));
		Assertions.assertEquals("company", user.getCompany());
	}

	@Test
	void deleteGroup() {
		Assertions.assertTrue(groups.containsKey("group"));
		Assertions.assertTrue(user.getGroups().contains("group"));

		repository.delete(groups.get("group"));

		Assertions.assertFalse(groups.containsKey("group"));
		Assertions.assertFalse(user.getGroups().contains("group"));
	}

	@Test
	void empty() {
		Assertions.assertEquals(1, user.getGroups().size());
		Assertions.assertTrue(users.containsKey("u"));
		Assertions.assertTrue(groups.get("group").getMembers().contains("u"));

		repository.empty(groups.get("group"), users);

		Mockito.verify(repository.cache).empty(groups.get("group"));
		Assertions.assertTrue(groups.get("group").getMembers().isEmpty());
		Assertions.assertEquals(0, user.getGroups().size());
	}

	@Test
	void deleteUser() {
		Assertions.assertEquals(1, user.getGroups().size());
		Assertions.assertTrue(users.containsKey("u"));

		repository.delete(user);

		Mockito.verify(repository.cache).delete(user);
		Assertions.assertFalse(users.containsKey("u"));
	}
}
