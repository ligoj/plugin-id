/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.iam.*;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.*;
import org.ligoj.app.plugin.id.dao.PasswordResetAuditRepository;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.ligoj.bootstrap.model.system.SystemAuthorization;
import org.ligoj.bootstrap.model.system.SystemAuthorization.AuthorizationType;
import org.ligoj.bootstrap.model.system.SystemRole;
import org.ligoj.bootstrap.model.system.SystemRoleAssignment;
import org.ligoj.bootstrap.model.system.SystemUser;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.internal.verification.VerificationModeFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

/**
 * Test of {@link UserOrgResource}<br>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class UserOrgResourceTest extends AbstractAppTest {

	private UserOrgResource resource;
	protected IUserRepository userRepository;
	protected IGroupRepository groupRepository;
	protected ICompanyRepository companyRepository;

	@Autowired
	private PasswordResetAuditRepository passwordResetAuditRepository;

	@Autowired
	private DelegateOrgRepository delegateOrgRepository;

	@BeforeEach
	void prepareData() throws IOException {
		persistEntities("csv", new Class<?>[]{DelegateOrg.class, CacheCompany.class, CacheGroup.class, CacheUser.class,
				CacheMembership.class}, StandardCharsets.UTF_8);
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
		resource.setIamProvider(new IamProvider[]{iamProvider});
		Mockito.when(companyRepository.getTypeName()).thenReturn("company");
	}

	@Override
	protected UriInfo newUriInfoAsc(final String ascProperty) {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORTED_COLUMN, "2");
		uriInfo.getQueryParameters().add("columns[2][data]", ascProperty);
		uriInfo.getQueryParameters().add(DataTableAttributes.SORT_DIRECTION, "asc");
		return uriInfo;
	}

	@Test
	void findById() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		resource.groupResource = Mockito.mock(GroupResource.class);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(newUser());
		Mockito.when(resource.groupResource.getContainers()).thenReturn(Collections.singleton(groupOrg1));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(Collections.singleton(groupOrg1));
		Assertions.assertNull(checkUser(resource.findById("WuSER")).getDn()); // Secured
		// data
	}

	@Test
	void findByIdNoCache() {
		Mockito.when(userRepository.findByIdNoCache("wuser")).thenReturn(newUser());
		Assertions.assertEquals("uid=wuser,ou=ing,ou=france,ou=people,dc=sample,dc=com",
				checkUser(resource.findByIdNoCache("WusER")).getDn());
	}

	@Test
	void findAllBy() {
		final UserOrg userOrg = new UserOrg();
		Mockito.when(userRepository.findAllBy("mail", "marc.martin@sample.com"))
				.thenReturn(Collections.singletonList(userOrg));
		Assertions.assertSame(userOrg, resource.findAllBy("mail", "marc.martin@sample.com").get(0));
	}

	@Test
	void findAll() {
		final var users = new HashMap<String, UserOrg>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final var groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=dig rha,cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		resource.groupResource = Mockito.mock(GroupResource.class);
		resource.companyResource = Mockito.mock(CompanyResource.class);
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.companyResource.getContainers()).thenReturn(Collections.singleton(company));
		Mockito.when(resource.companyResource.getContainersForWrite()).thenReturn(Collections.singleton(company));

		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", "dig rha", "iRsT", newUriInfoAsc("id"));
		Assertions.assertEquals(2, tableItem.getRecordsTotal());
		Assertions.assertEquals(2, tableItem.getRecordsFiltered());

		// Check the users
		final UserOrgVo userVo = checkUser(tableItem.getData().get(0));
		Assertions.assertTrue(userVo.getGroups().get(0).isCanWrite());
		Assertions.assertTrue(userVo.isCanWrite());
		Assertions.assertTrue(userVo.isCanWriteGroups());
	}

	@Test
	void findAllFilteredNonVisibleGroup() {
		final var users = new HashMap<String, UserOrg>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final var groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=dig rha,cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		resource.groupResource = Mockito.mock(GroupResource.class);
		resource.companyResource = Mockito.mock(CompanyResource.class);
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.companyResource.getContainers()).thenReturn(Collections.singleton(company));
		Mockito.when(resource.companyResource.getContainersForWrite()).thenReturn(Collections.singleton(company));

		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", "not exist group", "iRsT", newUriInfoAsc("id"));
		Assertions.assertEquals(2, tableItem.getRecordsTotal());
		Assertions.assertEquals(2, tableItem.getRecordsFiltered());

		// Check the users
		final UserOrgVo userVo = checkUser(tableItem.getData().get(0));
		Assertions.assertTrue(userVo.isCanWrite());
		Assertions.assertTrue(userVo.isCanWriteGroups());
	}

	@Test
	void findAllReadOnly() {
		initSpringSecurityContext("fdaugan");
		final var users = new HashMap<String, UserOrg>();
		final UserOrg user1 = newUser();
		user1.setCompany("ligoj");
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final var groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=dig rha,cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		resource.groupResource = Mockito.mock(GroupResource.class);
		resource.companyResource = Mockito.mock(CompanyResource.class);
		final var company1 = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var company2 = new CompanyOrg("ou=ligoj,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company1);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ligoj")).thenReturn(company2);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(Collections.emptySet());
		Mockito.when(resource.companyResource.getContainers())
				.thenReturn(new HashSet<>(Arrays.asList(company1, company2)));
		Mockito.when(resource.companyResource.getContainersForWrite()).thenReturn(Collections.emptySet());

		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", "not exist group", "iRsT", newUriInfoAsc("id"));
		Assertions.assertEquals(2, tableItem.getRecordsTotal());
		Assertions.assertEquals(2, tableItem.getRecordsFiltered());

		// Check the users
		Assertions.assertEquals("ligoj", tableItem.getData().get(0).getCompany());
		Assertions.assertFalse(tableItem.getData().get(0).getGroups().get(0).isCanWrite());
	}

	@Test
	void findAllNotVisibleCompany() {
		final var users = new HashMap<String, UserOrg>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		resource.groupResource = Mockito.mock(GroupResource.class);
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));

		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", "dig rha", "iRsT", newUriInfoAsc("id"));
		Assertions.assertEquals(2, tableItem.getRecordsTotal());
		Assertions.assertEquals(2, tableItem.getRecordsFiltered());

		// Check the users
		final UserOrgVo userVo = checkUser(tableItem.getData().get(0));
		Assertions.assertFalse(userVo.isCanWrite());
		Assertions.assertTrue(userVo.isCanWriteGroups());
	}

	private UserOrg newUser(final Consumer<UserOrg> consumerOld) {
		final UserOrg user1 = newUser();
		consumerOld.accept(user1);
		return user1;
	}

	private UserOrg newUser() {
		final UserOrg user1 = new UserOrg();
		user1.setDn("uid=wuser,ou=ing,ou=france,ou=people,dc=sample,dc=com");
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

	private <T extends SimpleUserOrg> void checkUser(T user) {
		// Check the other attributes
		Assertions.assertEquals("ing", user.getCompany());
		Assertions.assertEquals("First2", user.getFirstName());
		Assertions.assertEquals("Doe2", user.getLastName());
		Assertions.assertEquals("department1", user.getDepartment());
		Assertions.assertEquals("wuser", user.getId());
		Assertions.assertEquals("local1", user.getLocalId());
		Assertions.assertNotNull(user.getLocked());
		Assertions.assertEquals("user2", user.getLockedBy());
		Assertions.assertEquals("old-company", user.getIsolated());
		Assertions.assertEquals("wuser", user.getName());
		Assertions.assertEquals("first2.doe2@ing.fr", user.getMails().get(0));
	}

	private UserOrgVo checkUser(UserOrgVo user) {
		checkUser((SimpleUserOrg) user);
		final List<GroupVo> groups = new ArrayList<>(user.getGroups());
		Assertions.assertEquals(1, groups.size());
		Assertions.assertEquals("DIG", groups.get(0).getName());
		return user;
	}

	private UserOrg checkUser(UserOrg user) {
		checkUser((SimpleUserOrg) user);
		final List<String> groups = new ArrayList<>(user.getGroups());
		Assertions.assertEquals(1, groups.size());
		Assertions.assertEquals("dig", groups.get(0).toLowerCase());
		return user;
	}

	@Test
	void createNotWriteInCompany() {
		initSpringSecurityContext("any");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected("any", "ing")).thenReturn(company);
		groupFindById("any", "dig", groupOrg1);
		groupFindById("any", "dig rha", groupOrg2);

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rHA");
		user.setGroups(groups);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(user)), "company", "read-only");
	}

	@Test
	void createNotVisibleCompany() {
		initSpringSecurityContext("any");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected("any", "ing")).thenReturn(company);
		groupFindById("any", "dig", groupOrg1);
		groupFindById("any", "dig rha", groupOrg2);

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rHA");
		user.setGroups(groups);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(user)), "company", "read-only");
	}

	@Test
	void create() {
		final var user = prepareUserOrgEdition();
		Assertions.assertNull(resource.create(user));
	}

	private UserOrgEditionVo prepareUserOrgEdition() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		groupFindById(DEFAULT_USER, "dig rha", groupOrg2);

		final var user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		user.setGroups(List.of("dig rHA"));
		return user;
	}

	@Test
	void createWithPassword() {
		final var user = prepareUserOrgEdition();
		user.setReturnGeneratePassword(true);
		injectPasswordGenerator("flasta", "my-secret");
		final var password = resource.create(user);
		Assertions.assertEquals("my-secret", password);
	}

	@Test
	void updatePassword() {
		resource.applicationContext = Mockito.mock(ApplicationContext.class);
		final var generator = Mockito.mock(IPasswordGenerator.class);
		Mockito.when(resource.applicationContext.getBeansOfType(IPasswordGenerator.class))
				.thenReturn(Collections.singletonMap("bean", generator));
		resource.updatePassword(newUser(), false);
		Mockito.verify(generator, VerificationModeFactory.atLeast(1)).generate("wuser", false);
	}

	private IPasswordGenerator injectPasswordGenerator(final String user, final String password) {
		resource.applicationContext = Mockito.mock(ApplicationContext.class);
		final var generator = Mockito.mock(IPasswordGenerator.class);
		Mockito.when(resource.applicationContext.getBeansOfType(IPasswordGenerator.class))
				.thenReturn(Collections.singletonMap("bean", generator));
		Mockito.doReturn(password).when(generator).generate(user, false);
		return generator;
	}

	@Test
	void resetPassword() {
		prepareUser();
		Assertions.assertEquals(0, passwordResetAuditRepository.countBy("login", "wuser"));
		final var generator = injectPasswordGenerator("wuser", "my-secret");
		resource.resetPassword("wuser");
		resource.resetPassword("wuser");
		Mockito.verify(generator, VerificationModeFactory.atLeast(2)).generate("wuser", false);

		// Check the audit
		Assertions.assertEquals(2, passwordResetAuditRepository.countBy("login", "wuser"));
		final var last = passwordResetAuditRepository.findBy("login", "wuser");
		Assertions.assertEquals(getAuthenticationName(), last.getCreatedBy());
		Assertions.assertNotNull(last.getCreatedDate());
		Assertions.assertEquals("wuser", last.getLogin());
	}

	@Test
	void resetPasswordNoPasswordGenerator() {
		prepareUser();
		Assertions.assertEquals(0, passwordResetAuditRepository.countBy("login", "wuser"));
		resource.applicationContext = Mockito.mock(ApplicationContext.class);
		Mockito.when(resource.applicationContext.getBeansOfType(IPasswordGenerator.class))
				.thenReturn(Collections.emptyMap());
		Assertions.assertNull(resource.resetPassword("wuser"));

		// Check the audit: no write since no password generator
		Assertions.assertEquals(0, passwordResetAuditRepository.countBy("login", "wuser"));
	}

	private void prepareUser() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setId("wuser");
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig rha"));

		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
	}

	@Test
	void createUserAlreadyExists() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final var userOrg = new UserOrg();
		userOrg.setCompany("ing");
		userOrg.setGroups(Collections.singleton("dig rha"));
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("flasta")).thenReturn(userOrg);
		Mockito.when(userRepository.findById("flasta")).thenReturn(userOrg);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		groupFindById(DEFAULT_USER, "dig rha", groupOrg2);

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		user.setGroups(new ArrayList<>());
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.create(user)), "id", "already-exist");
	}

	@Test
	void deleteLastMember() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.delete("wuser")), "id", "last-member-of-group");
	}

	@Test
	void deleteUserNoWriteCompany() {
		initSpringSecurityContext("mtuyer");
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				new HashSet<>(Arrays.asList("wuser", "user1")));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected("mtuyer", "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.delete("wuser")), "id", "read-only");
	}

	@Test
	void deleteUserNotVisibleUser() {
		initSpringSecurityContext("mtuyer");
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				new HashSet<>(Arrays.asList("wuser", "user1")));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		Mockito.when(userRepository.findByIdExpected("mtuyer", "wuser")).thenThrow(
				new ValidationJsonException("id", BusinessException.KEY_UNKNOWN_ID, "0", "user", "1", "mtuyer"));
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.delete("wuser")), "id", BusinessException.KEY_UNKNOWN_ID);
	}

	@Test
	void resetPasswordUserNoWriteCompany() {
		initSpringSecurityContext("mtuyer");
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				new HashSet<>(Arrays.asList("wuser", "user1")));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected("mtuyer", "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.resetPassword("wuser")), "id", "read-only");
	}

	@Test
	void mergeUserNoChange() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(groupRepository.findByDepartment("department1")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByDepartment("department2")).thenReturn(groupOrg2);

		final UserOrg newUser = newUser();

		resource.mergeUser(newUser(), newUser);
		Assertions.assertEquals("department1", newUser.getDepartment());
		Assertions.assertEquals("local1", newUser.getLocalId());
	}

	@Test
	void mergeUserNoDepartment() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(groupRepository.findByDepartment("department1")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByDepartment("department2")).thenReturn(groupOrg2);

		final UserOrg userOrg2 = newUser();
		final UserOrg newUser = newUser();
		newUser.setDepartment(null);
		newUser.setLocalId(null);
		resource.mergeUser(userOrg2, newUser);
		Assertions.assertNull(userOrg2.getDepartment());
		Assertions.assertNull(userOrg2.getLocalId());
	}

	@Test
	void mergeUser() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(groupRepository.findByDepartment("department1")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByDepartment("department2")).thenReturn(groupOrg2);

		final UserOrg userOrg2 = newUser();
		final UserOrg newUser = newUser();
		newUser.setDepartment("department2");
		newUser.setLocalId("local2");
		resource.mergeUser(userOrg2, newUser);
		Assertions.assertEquals("department2", userOrg2.getDepartment());
		Assertions.assertEquals("local2", userOrg2.getLocalId());
	}

	/**
	 * Update everything : attributes and mails
	 */
	@Test
	void update() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		final var user = newUser();
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
		resource.update(userVo);
	}

	@Test
	void updateNotWriteInCompany() {
		initSpringSecurityContext("mtuyer");
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		final var user = newUser();
		Mockito.when(userRepository.findByIdExpected("mtuyer", "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(companyRepository.findByIdExpected("mtuyer", "ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);

		// Remove all write permission of this user
		delegateOrgRepository.findAllByUser("mtuyer").forEach(d -> d.setCanWrite(false));

		final UserOrgEditionVo userVo = new UserOrgEditionVo();
		userVo.setId("wuser");
		userVo.setFirstName("FirstA");
		userVo.setLastName("LastA");
		userVo.setCompany("ing");
		userVo.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.update(userVo)), "company", "read-only");
	}

	@Test
	void updateFirstName() {
		// First name change only
		update2(userVo -> userVo.setFirstName("XFirst2"));
	}

	private void update2(Consumer<UserOrgEditionVo> consumer) {
		update2(consumer, c -> {
			// No change
		});
	}

	private void update2(Consumer<UserOrgEditionVo> consumerNew, Consumer<UserOrg> consumerOld) {

		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var company2 = new CompanyOrg("ou=ligoj,ou=france,ou=people,dc=sample,dc=com", "ligoj");
		final var groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=dig rha,cn=dig as,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		final var groupOrg3 = new GroupOrg("cn=other,dc=other,dc=com", "Other", Collections.singleton("user2"));
		final var groupOrg4 = new GroupOrg("cn=invisible,dc=net", "Other", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		groupsMap.put("other", groupOrg3);
		groupsMap.put("invisible", groupOrg4);
		final var user = newUser(consumerOld);
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(companyRepository.findById("ligoj")).thenReturn(company2);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ligoj")).thenReturn(company2);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		groupFindById(DEFAULT_USER, "dig rha", groupOrg2);
		groupFindById(DEFAULT_USER, "other", groupOrg3);
		Mockito.when(groupRepository.findById("invisible")).thenReturn(groupOrg4);
		Mockito.when(groupRepository.findByDepartment("department1")).thenReturn(groupOrg1);
		Mockito.when(groupRepository.findByDepartment("department2")).thenReturn(groupOrg2);

		final UserOrgEditionVo userVo = new UserOrgEditionVo();
		userVo.setId("wuser");
		userVo.setFirstName("First2");
		userVo.setLastName("Doe2");
		userVo.setDepartment("department1");
		userVo.setLocalId("local1");
		userVo.setMail("first2.doe2@ing.fr");
		userVo.setCompany("ing");
		userVo.setGroups(Collections.singletonList("dig"));
		consumerNew.accept(userVo);
		resource.update(userVo);
	}

	@Test
	void updateLastName() {
		// Last name change only
		update2(userVo -> userVo.setLastName("XDoe2"));
	}

	@Test
	void updateMail() {
		// Mail change only
		update2(userVo -> userVo.setMail("john31.last31@ing.com"));
	}

	@Test
	void updateCompany() {
		update2(userVo -> userVo.setCompany("ligoj"));
	}

	@Test
	void updateUserChangeDepartment() {
		update2(userVo -> userVo.setDepartment("department2"));
	}

	@Test
	void updateUserChangeDepartmentNotExists() {
		update2(userVo -> userVo.setDepartment("any"));
	}

	@Test
	void updateUserNoChange() {
		update2(userVo -> {
			// No change
		});
	}

	@Test
	void updateUserHadNoMail() {
		update2(userVo -> userVo.setFirstName("XFirstA"), userVo -> userVo.setMails(new ArrayList<>()));
	}

	@Test
	void updateUserHasNoMail() {
		update2(userVo -> userVo.setMail(null));
	}

	@Test
	void updateUserWasNotSecured() {
		update2(userVo -> userVo.setFirstName("XFirstA"), userVo -> userVo.setSecured(false));
	}

	@Test
	void updateGroup() {
		// Remove group "dig"
		update2(userVo -> userVo.setGroups(Arrays.asList("other", "dig rha")),
				u -> u.setGroups(Arrays.asList("other", "dig rha", "dig")));
	}

	@Test
	void updateGroupRemoveWithInvisible() {
		// Remove group "dig" when there is an invisible group
		update2(userVo -> userVo.setGroups(Arrays.asList("other", "dig rha")),
				u -> u.setGroups(Arrays.asList("invisible", "other", "dig rha", "dig")));
	}

	@Test
	void findAllNotSecureByCompany() {
		findAllByCommon(true, false, "ing");
	}

	@Test
	void findAllNotSecureByVisibleCompany() {
		findAllByCommon(false, true, "ing");
	}

	private void findAllByCommon(final boolean mockGroups, final boolean mockCompanies, final String search) {
		final var users = new HashMap<String, UserOrg>();
		final var user1 = newUser();
		users.put("wuser", user1);
		final var user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		if (mockGroups) {
			resource.groupResource = Mockito.mock(GroupResource.class);
			final var groupsMap = new HashMap<String, GroupOrg>();
			groupsMap.put("dig", groupOrg1);
			Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
			Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));
		}
		if (mockCompanies) {
			resource.companyResource = Mockito.mock(CompanyResource.class);
			Mockito.when(resource.companyResource.getContainers()).thenReturn(Collections.singleton(company));
			Mockito.when(resource.companyResource.getContainersForWrite()).thenReturn(Collections.singleton(company));
		}
		final var data = resource.findAllNotSecure(search, null);

		// Check the users
		checkUser(data.get(0));
	}

	@Test
	void findAllNotSecureByGroup() {
		findAllByCommon(true, false, "dig");
	}

	@Test
	void findAllNotSecureByVisibleGroup() {
		findAllByCommon(false, true, "dig");
	}

	@Test
	void findAllNotSecure() {
		findAllByCommon(true, false, null);
	}

	@Test
	void lock() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig rha"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.lock("wuser");
	}

	@Test
	void isolate() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig rha"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.isolate("wuser");
	}

	@Test
	void restore() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setCompany("ing");
		user.setIsolated("ing");
		user.setGroups(Collections.singleton("dig rha"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.restore("wuser");
	}

	@Test
	void unlock() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.unlock("wuser");
	}

	@Test
	void delete() {
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				new HashSet<>(Arrays.asList("wuser", "user1")));
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		final var user = new UserOrg();
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
	void addUserToGroup() {
		mockAddUser(DEFAULT_USER);
		resource.addUserToGroup("wuser", "dig rha");
	}

	/**
	 * Add a user to a group without delegates but being system administrator.
	 */
	@Test
	void addUserToGroupSystemAdmin() {
		initSpringSecurityContext("my-admin");
		var user = new SystemUser();
		user.setLogin("my-admin");
		em.persist(user);
		var role = new SystemRole();
		role.setName("some");
		em.persist(role);
		var authorization = new SystemAuthorization();
		authorization.setType(AuthorizationType.API);
		authorization.setPattern(".*");
		authorization.setRole(role);
		em.persist(authorization);
		var assignment = new SystemRoleAssignment();
		assignment.setRole(role);
		assignment.setUser(user);
		em.persist(assignment);
		mockAddUser("my-admin");
		resource.addUserToGroup("wuser", "dig rha");
	}

	private void mockAddUser(final String principal) {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final var user = newUser(u -> u.setGroups(List.of("dig")));
		final var company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ligoj");
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		groupFindById(principal, "dig", groupOrg1);
		groupFindById(principal, "dig rha", groupOrg2);
	}

	/**
	 * Add a user to a group this user is already member
	 */
	@Test
	void addUserToGroupAlreadyMember() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var user = newUser();
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		groupFindById(DEFAULT_USER, "dig rha", groupOrg2);
		resource.addUserToGroup("wuser", "dig");
	}

	/**
	 * Add a user to a group the principal does not visible.
	 */
	@Test
	void addUserToGroupNotWritableGroup() {
		initSpringSecurityContext("mlavoine");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final var user = new UserOrg();
		user.setCompany("ligoj");
		user.setGroups(Collections.singleton("dig rha"));
		final var company = new CompanyOrg("ou=ligoj,ou=france,ou=people,dc=sample,dc=com", "ligoj");
		Mockito.when(companyRepository.findById("ligoj")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		groupFindById("mlavoine", "dig", groupOrg1);
		groupFindById("mlavoine", "dig rha", groupOrg2);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class,
				() -> resource.addUserToGroup("wuser", "dig")), "group", "read-only");
	}

	/**
	 * Remove a user to a group the principal does not visible.
	 */
	@Test
	void removeUserFromGroupNotWritableGroup() {
		initSpringSecurityContext("mlavoine");
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final var user = new UserOrg();
		user.setCompany("ligoj");
		user.setGroups(Collections.singleton("dig rha"));
		final var company = new CompanyOrg("ou=ligoj,ou=france,ou=people,dc=sample,dc=com", "ligoj");
		Mockito.when(companyRepository.findById("ligoj")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		groupFindById("mlavoine", "dig", groupOrg1);
		groupFindById("mlavoine", "dig rha", groupOrg2);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () ->
				resource.removeUserFromGroup("wuser", "dig rha")), "group", "read-only");
	}

	/**
	 * Test user addition to a group.
	 */
	@Test
	void removeUserFromGroup() {
		final var groupOrg1 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		final var groupOrg2 = new GroupOrg("cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG AS",
				Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final var user = newUser(u -> u.setGroups(Arrays.asList("dig rha", "dig as")));
		final var company = new CompanyOrg("ou=ligoj,ou=france,ou=people,dc=sample,dc=com", "ligoj");
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		groupFindById(DEFAULT_USER, "dig rha", groupOrg1);
		groupFindById(DEFAULT_USER, "dig as", groupOrg2);
		resource.removeUserFromGroup("wuser", "dig rha");
	}

	private void groupFindById(final String user, final String id, final GroupOrg group) {
		Mockito.when(groupRepository.findByIdExpected(user, id)).thenReturn(group);
		Mockito.when(groupRepository.findById(user, id)).thenReturn(group);
		Mockito.when(groupRepository.findById(id)).thenReturn(group);
	}

	@Test
	void canWriteNotSameType() {
		final var delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assertions.assertFalse(resource.canWrite(delegate, null, DelegateType.COMPANY));
	}

	@Test
	void canWriteSameTypeNoRight() {
		final var delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assertions.assertFalse(resource.canWrite(delegate, null, DelegateType.GROUP));
	}

	@Test
	void canWrite() {
		final var delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setDn("rightdn");
		delegate.setCanWrite(true);
		Assertions.assertTrue(resource.canWrite(delegate, "rightdn", DelegateType.GROUP));
	}

	/**
	 * Admin on delegate does not grant write access.
	 */
	@Test
	void canWriteAsAdmin() {
		final var delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setCanAdmin(true);
		delegate.setDn("rightdn");
		Assertions.assertFalse(resource.canWrite(delegate, "rightdn", DelegateType.GROUP));
	}

	@Test
	void canWriteAsReader() {
		final var delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setDn("rightdn");
		Assertions.assertFalse(resource.canWrite(delegate, "rightdn", DelegateType.GROUP));
	}

}
