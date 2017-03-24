package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.CompanyOrg;
import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.api.SimpleUserOrg;
import org.ligoj.app.api.UserOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test of {@link UserOrgResource}<br>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@org.junit.FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserOrgResourceTest extends AbstractAppTest {

	private UserOrgResource resource;
	protected IUserRepository userRepository;
	protected IGroupRepository groupRepository;
	protected ICompanyRepository companyRepository;

	@Before
	public void prepareData() throws IOException {
		persistEntities("csv/app-test",
				new Class[] { DelegateOrg.class, CacheCompany.class, CacheGroup.class, CacheUser.class, CacheMembership.class },
				StandardCharsets.UTF_8.name());
		iamProvider = Mockito.mock(IamProvider.class);
		final IamConfiguration configuration = Mockito.mock(IamConfiguration.class);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(configuration);
		userRepository = Mockito.mock(IUserRepository.class);
		groupRepository = Mockito.mock(IGroupRepository.class);
		companyRepository = Mockito.mock(ICompanyRepository.class);
		Mockito.when(configuration.getUserRepository()).thenReturn(userRepository);
		Mockito.when(configuration.getCompanyRepository()).thenReturn(companyRepository);
		Mockito.when(configuration.getGroupRepository()).thenReturn(groupRepository);
		resource = new UserOrgResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.setIamProvider(iamProvider);
		Mockito.when(companyRepository.getTypeName()).thenReturn("company");
	}

	private UriInfo newUriInfoAsc(final String ascProperty) {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORTED_COLUMN, "2");
		uriInfo.getQueryParameters().add("columns[2][data]", ascProperty);
		uriInfo.getQueryParameters().add(DataTableAttributes.SORT_DIRECTION, "asc");
		return uriInfo;
	}

	@Test
	public void findById() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig")).thenReturn(groupOrg1);
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(newUser());
		checkUser(resource.findById("WuSER"));
	}

	@Test
	public void findByIdNoCache() {
		Mockito.when(userRepository.findByIdNoCache("wuser")).thenReturn(newUser());
		checkUser(resource.findByIdNoCache("WusER"));
	}

	@Test
	public void authenticate() {
		Assert.assertFalse(resource.authenticate("fdaugan", "-bad-"));
	}

	@Test
	public void findAllBy() {
		final UserOrg userOrg = new UserOrg();
		Mockito.when(userRepository.findAllBy("mail", "marc.martin@sample.com")).thenReturn(Collections.singletonList(userOrg));
		Assert.assertSame(userOrg, resource.findAllBy("mail", "marc.martin@sample.com").get(0));
	}

	@Test
	public void findAll() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findById("dig")).thenReturn(groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(new PageImpl<>(new ArrayList<>(users.values())));

		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", "dig rha", "iRsT", newUriInfoAsc("id"));
		Assert.assertEquals(2, tableItem.getRecordsTotal());
		Assert.assertEquals(2, tableItem.getRecordsFiltered());

		// Check the users
		checkUser(tableItem.getData().get(0));
		Assert.assertEquals("user2", tableItem.getData().get(1).getId());
	}

	private UserOrg newUser() {
		final UserOrg user1 = new UserOrg();
		user1.setId("wuser");
		user1.setFirstName("First2");
		user1.setLastName("Doe2");
		user1.setDepartment("department1");
		user1.setLocalId("local1");
		user1.setMails(Collections.singletonList("first2.doe2@ing.fr"));
		user1.setLocked(new Date());
		user1.setLockedBy("user2");
		user1.setIsolated("old-company");
		user1.setSecured(true);
		user1.setCompany("ing");
		user1.setGroups(Collections.singletonList("dig"));
		return user1;
	}

	private void checkUser(SimpleUserOrg user) {

		// Check the other attributes
		Assert.assertEquals("ing", user.getCompany());
		Assert.assertEquals("First2", user.getFirstName());
		Assert.assertEquals("Doe2", user.getLastName());
		Assert.assertEquals("department1", user.getDepartment());
		Assert.assertEquals("wuser", user.getId());
		Assert.assertEquals("local1", user.getLocalId());
		Assert.assertNotNull(user.getLocked());
		Assert.assertEquals("locker", user.getLockedBy());
		Assert.assertEquals("Doe2", user.getIsolated());
		Assert.assertEquals("Doe2", user.getName());
		Assert.assertEquals("first2.doe2@ing.fr", user.getMails().get(0));
	}

	private void checkUser(UserOrgVo user) {
		checkUser((SimpleUserOrg) user);
		Assert.assertTrue(user.isManaged());
		final List<GroupLdapVo> groups = new ArrayList<>(user.getGroups());
		Assert.assertEquals(1, groups.size());
		Assert.assertEquals("DIG", groups.get(0).getName());
	}

	private void checkUser(UserOrg user) {
		checkUser((SimpleUserOrg) user);
		Assert.assertEquals("dn", user.getDn());
		final List<String> groups = new ArrayList<>(user.getGroups());
		Assert.assertEquals(1, groups.size());
		Assert.assertEquals("DIG", groups.get(0));
	}

	@Test
	public void createNotWriteInCompany() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "unknown-id"));
		initSpringSecurityContext("any");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("flasta"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected("any", "ing")).thenReturn(company);
		Mockito.when(groupRepository.findByIdExpected("any", "dig")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByIdExpected("any", "dig rha")).thenReturn(groupOrg2);
		Mockito.when(groupRepository.findById("dig rha")).thenReturn(groupOrg2);

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rHA");
		user.setGroups(groups);
		resource.create(user);
	}

	@Test
	public void create() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("flasta"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig rha")).thenReturn(groupOrg2);
		Mockito.when(groupRepository.findById("dig rha")).thenReturn(groupOrg2);

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rHA");
		user.setGroups(groups);
		resource.create(user);
	}

	@Test
	public void createUserAlreadyExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", "already-exist"));
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("flasta"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		final UserOrg userOrg = new UserOrg();
		userOrg.setCompany("ing");
		userOrg.setGroups(Collections.singleton("dig rha"));
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("flasta")).thenReturn(userOrg);
		Mockito.when(userRepository.findById("flasta")).thenReturn(userOrg);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig rha")).thenReturn(groupOrg2);
		Mockito.when(groupRepository.findById("dig rha")).thenReturn(groupOrg2);

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		user.setGroups(new ArrayList<>());
		resource.create(user);
	}

	@Test
	public void deleteLastMember() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", "last-member-of-group"));
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		resource.delete("wuser");
	}

	@Test
	public void deleteUserNoWriteCompany() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", BusinessException.KEY_UNKNOW_ID));
		initSpringSecurityContext("mtuyer");
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				new HashSet<>(Arrays.asList("wuser", "user1")));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected("mtuyer", "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.delete("wuser");
	}

	@Test
	public void mergeUserNoChange() {
		final UserOrg userLdap2 = getUser().findById("flast1");
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());

		resource.mergeUser(userLdap2, new UserOrg());
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());
	}

	@Test
	public void mergeUser() {
		final UserOrg userLdap2 = getUser().findById("flast1");
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());

		final UserOrg newUser = new UserOrg();
		newUser.setDepartment("any");
		newUser.setLocalId("some");
		resource.mergeUser(userLdap2, newUser);
		Assert.assertEquals("any", userLdap2.getDepartment());
		Assert.assertEquals("some", userLdap2.getLocalId());

		// Revert to previous state (null)
		resource.mergeUser(userLdap2, new UserOrg());
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());
	}

	/**
	 * Update everything : attributes and mails
	 */
	@Test
	public void update() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);

		final UserOrgEditionVo userVo = new UserOrgEditionVo();
		userVo.setId("wuser");
		userVo.setFirstName("FirstA");
		userVo.setLastName("LastA");
		userVo.setCompany("ing");
		userVo.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(userVo);
	}

	@Test
	public void updateFirstName() {
		// First name change only
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("jlast3");
		user.setFirstName("John31");
		user.setLastName("Last3");
		user.setCompany("ing");
		user.setMail("john3.last3@ing.com");
		user.setGroups(null);
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test
	public void updateLastName() {
		// Last name change only
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("jlast3");
		user.setFirstName("John31");
		user.setLastName("Last31");
		user.setCompany("ing");
		user.setMail("john3.last3@ing.com");
		user.setGroups(null);
		user.setGroups(null);
		resource.update(user);
	}

	@Test
	public void updateMail() {
		// Mail change only
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("jlast3");
		user.setFirstName("John31");
		user.setLastName("Last31");
		user.setCompany("ing");
		user.setMail("john31.last31@ing.com");
		user.setGroups(null);
		resource.update(user);
	}

	@Test
	public void updateUserChangeCompanyAndBackAgain() {

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("ing"); // Previous is "socygan"
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test(expected = ValidationJsonException.class)
	public void updateUserChangeDepartmentNotVisible() {
		initSpringSecurityContext("assist");

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("socygan"); // Unchanged
		user.setDepartment("456987"); // Previous is null -> "DIG AS" (not visible)
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		resource.update(user);
	}

	@Test
	public void updateUserChangeDepartmentAndBackAgain() {
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("socygan"); // Unchanged
		user.setDepartment("456987"); // Previous is null -> "DIG AS"
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		resource.update(user);
	}

	@Test
	public void updateUserChangeDepartmentNotExists() {
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("socygan"); // Unchanged
		user.setDepartment("any"); // Previous is null -> No linked group exists
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test
	public void updateUserCompanyNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("any");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserGroupNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("groups", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast1");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("any");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoChange() {
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("jlast3");
		user.setFirstName("John3");
		user.setLastName("Last3");
		user.setCompany("ing");
		user.setMail("jlast3@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegate() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast1");
		user.setFirstName("FirstW");
		user.setLastName("LastW");
		user.setCompany("ing");
		user.setMail("flastw@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("any");

		resource.update(user);
	}

	@Test
	public void updateUserReadOnly() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("groups", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("First0");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.last0@socygan.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("Biz Agency");
		user.setGroups(groups);
		initSpringSecurityContext("mlavoine");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompany() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("socygan");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("any");

		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompanyChangeFirstName() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("FirstA");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.last0@socygan.fr");
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompanyChangeMail() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("First0");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.lastA@socygan.fr");
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompanyNoChange() {
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast0");
		user.setFirstName("First0");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.last0@socygan.fr");
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateGroupForTarget() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("groups", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast1");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig sud ouest"); // no right on this group
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserHadNoMail() {
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("jdoe5");
		user.setFirstName("John5");
		user.setLastName("Doe5");
		user.setCompany("ing");
		user.setMail("first5.last5@ing.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserHasNoMail() {
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("jdoe5");
		user.setFirstName("John5");
		user.setLastName("Doe5");
		user.setCompany("ing");
		user.setMail(null);
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoPassword() {
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("jdoe4");
		user.setFirstName("John4");
		user.setLastName("Doe4");
		user.setCompany("ing");
		user.setMail("fohn4.doe4@ing.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", BusinessException.KEY_UNKNOW_ID));
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flast11");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test
	public void updateRemoveGroup() {
		// Remove group "Biz Agency"
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("fdoe2");
		user.setFirstName("First2");
		user.setLastName("Doe2");
		user.setCompany("ing");
		user.setMail("fdoe2@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("DIG RHA");
		user.setGroups(groups);
		resource.update(user);
	}

	/**
	 * Add a group to user having already some groups but not visible from the current user.
	 */
	@Test
	public void updateUserAddGroup() {

		// Add a new valid group "DIG RHA" to "wuser" by "fdaugan"
		initSpringSecurityContext("fdaugan");
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("wuser");
		user.setFirstName("William");
		user.setLastName("User");
		user.setCompany("ing");
		user.setMail("wuser.wuser@ing.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("DIG RHA");
		groups.add("Biz Agency Manager");
		user.setGroups(groups);
		resource.update(user);
	}

	@Test
	public void lock() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig rha"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.lock("wuser");
	}

	@Test
	public void isolate() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig rha"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.isolate("wuser");
	}

	@Test
	public void restore() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setIsolated("ing");
		user.setGroups(Collections.singleton("dig rha"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.restore("wuser");
	}

	@Test
	public void unlock() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.unlock("wuuser");
	}

	@Test
	public void delete() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				new HashSet<>(Arrays.asList("wuser", "user1")));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.delete("wuser");
	}

	/**
	 * Add a user to a group
	 */
	@Test
	public void addUserToGroup() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("user1"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		final UserOrg user = new UserOrg();
		user.setCompany("gfi");
		user.setGroups(Collections.singleton("dig rha"));
		final CompanyOrg company = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		Mockito.when(companyRepository.findById("gfi")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig rha")).thenReturn(groupOrg2);
		Mockito.when(groupRepository.findById("dig rha")).thenReturn(groupOrg2);
		resource.addUserToGroup("wuser", "dig");
	}

	/**
	 * Add a user to a group this user is already member
	 */
	@Test
	public void addUserToGroupAlreadyMember() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		final UserOrg user = new UserOrg();
		user.setCompany("gfi");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig")).thenReturn(groupOrg2);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig rha")).thenReturn(groupOrg2);
		resource.addUserToGroup("wuser", "dig");
	}

	/**
	 * Add a user to a group the principal does not manage.
	 */
	@Test
	public void addUserToGroupNotWritableGroup() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("group", BusinessException.KEY_UNKNOW_ID));
		initSpringSecurityContext("mlavoine");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("user1"));
		final UserOrg user = new UserOrg();
		user.setCompany("gfi");
		user.setGroups(Collections.singleton("dig rha"));
		final CompanyOrg company = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		Mockito.when(companyRepository.findById("gfi")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		Mockito.when(groupRepository.findByIdExpected("mlavoine", "dig")).thenReturn(groupOrg1);
		resource.addUserToGroup("wuser", "dig");
	}

	/**
	 * Test user addition to a group.
	 */
	@Test
	public void removeUser() {
		resource.removeUser("wuser", "dig rha");
	}

	@Test
	public void isGrantedNotSameType() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assert.assertFalse(resource.isGrantedAccess(delegate, null, DelegateType.COMPANY, true));
	}

	@Test
	public void isGrantedSameTypeNoRight() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assert.assertFalse(resource.isGrantedAccess(delegate, null, DelegateType.GROUP, true));
	}

	@Test
	public void isGrantedSameTypeNotSameDn() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assert.assertFalse(resource.isGrantedAccess(delegate, null, DelegateType.GROUP, false));
	}

	@Test
	public void isGranted() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setDn("rightdn");
		Assert.assertTrue(resource.isGrantedAccess(delegate, "rightdn", DelegateType.GROUP, false));
	}

	@Test
	public void isGrantedAsAdmin() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setCanAdmin(true);
		delegate.setDn("rightdn");
		Assert.assertTrue(resource.isGrantedAccess(delegate, "rightdn", DelegateType.GROUP, true));
	}

	@Test
	public void isGrantedAsWriter() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setCanWrite(true);
		delegate.setDn("rightdn");
		Assert.assertTrue(resource.isGrantedAccess(delegate, "rightdn", DelegateType.GROUP, true));
	}

}
