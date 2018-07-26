/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
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
import java.util.function.Consumer;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IPasswordGenerator;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.SimpleUserOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.plugin.id.dao.PasswordResetAuditRepository;
import org.ligoj.app.plugin.id.model.PasswordResetAudit;
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

/**
 * Test of {@link UserOrgResource}<br>
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class UserOrgResourceTest extends AbstractAppTest {

	private UserOrgResource resource;
	protected IUserRepository userRepository;
	protected IGroupRepository groupRepository;
	protected ICompanyRepository companyRepository;

	@Autowired
	private PasswordResetAuditRepository passwordResetAuditRepository;

	@Autowired
	private DelegateOrgRepository delegateOrgRepository;

	@BeforeEach
	public void prepareData() throws IOException {
		persistEntities("csv", new Class[] { DelegateOrg.class, CacheCompany.class, CacheGroup.class, CacheUser.class,
				CacheMembership.class }, StandardCharsets.UTF_8.name());
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
		resource.setIamProvider(new IamProvider[] { iamProvider });
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
	public void findById() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
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
	public void findByIdNoCache() {
		Mockito.when(userRepository.findByIdNoCache("wuser")).thenReturn(newUser());
		Assertions.assertEquals("uid=wuser,ou=ing,ou=france,ou=people,dc=sample,dc=com",
				checkUser(resource.findByIdNoCache("WusER")).getDn());
	}

	@Test
	public void findAllBy() {
		final UserOrg userOrg = new UserOrg();
		Mockito.when(userRepository.findAllBy("mail", "marc.martin@sample.com"))
				.thenReturn(Collections.singletonList(userOrg));
		Assertions.assertSame(userOrg, resource.findAllBy("mail", "marc.martin@sample.com").get(0));
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
		final GroupOrg groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=dig rha,cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		resource.groupResource = Mockito.mock(GroupResource.class);
		resource.companyResource = Mockito.mock(CompanyResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
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
	public void findAllFilteredNonVisibleGroup() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=dig rha,cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		resource.groupResource = Mockito.mock(GroupResource.class);
		resource.companyResource = Mockito.mock(CompanyResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
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
	public void findAllReadOnly() {
		initSpringSecurityContext("fdaugan");
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		user1.setCompany("gfi");
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=dig rha,cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		resource.groupResource = Mockito.mock(GroupResource.class);
		resource.companyResource = Mockito.mock(CompanyResource.class);
		final CompanyOrg company1 = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final CompanyOrg company2 = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company1);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "gfi")).thenReturn(company2);
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
		Assertions.assertEquals("gfi", tableItem.getData().get(0).getCompany());
		Assertions.assertFalse(tableItem.getData().get(0).getGroups().get(0).isCanWrite());
	}

	@Test
	public void findAllNotVisibleCompany() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		resource.groupResource = Mockito.mock(GroupResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
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

	private <T extends SimpleUserOrg> T checkUser(T user) {

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
		return user;
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
	public void createNotWriteInCompany() {
		initSpringSecurityContext("any");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
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
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.create(user);
		}), "company", "read-only");
	}

	@Test
	public void createNotVisibleCompany() {
		initSpringSecurityContext("any");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
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
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.create(user);
		}), "company", "read-only");
	}

	@Test
	public void create() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		groupFindById(DEFAULT_USER, "dig rha", groupOrg2);

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
	public void updatePassword() {
		resource.applicationContext = Mockito.mock(ApplicationContext.class);
		final IPasswordGenerator generator = Mockito.mock(IPasswordGenerator.class);
		Mockito.when(resource.applicationContext.getBeansOfType(IPasswordGenerator.class))
				.thenReturn(Collections.singletonMap("bean", generator));
		resource.updatePassword(newUser(), false);
		Mockito.verify(generator, VerificationModeFactory.atLeast(1)).generate("wuser", false);
	}

	@Test
	public void resetPassword() {
		prepareUser();
		Assertions.assertEquals(0, passwordResetAuditRepository.countBy("login", "wuser"));
		resource.applicationContext = Mockito.mock(ApplicationContext.class);
		final IPasswordGenerator generator = Mockito.mock(IPasswordGenerator.class);
		Mockito.when(resource.applicationContext.getBeansOfType(IPasswordGenerator.class))
				.thenReturn(Collections.singletonMap("bean", generator));
		Mockito.doReturn("my-secret").when(generator).generate("wuser", false);
		resource.resetPassword("wuser");
		resource.resetPassword("wuser");
		Mockito.verify(generator, VerificationModeFactory.atLeast(2)).generate("wuser", false);

		// Check the audit
		Assertions.assertEquals(2, passwordResetAuditRepository.countBy("login", "wuser"));
		final PasswordResetAudit last = passwordResetAuditRepository.findBy("login", "wuser");
		Assertions.assertEquals(getAuthenticationName(), last.getCreatedBy());
		Assertions.assertNotNull(last.getCreatedDate());
		Assertions.assertEquals("wuser", last.getLogin());
	}

	@Test
	public void resetPasswordNoPasswordGenerator() {
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
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setId("wuser");
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig rha"));

		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
	}

	@Test
	public void createUserAlreadyExists() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("flasta"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
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
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		groupFindById(DEFAULT_USER, "dig rha", groupOrg2);

		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		user.setGroups(new ArrayList<>());
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.create(user);
		}), "id", "already-exist");
	}

	@Test
	public void deleteLastMember() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.delete("wuser");
		}), "id", "last-member-of-group");
	}

	@Test
	public void deleteUserNoWriteCompany() {
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
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.delete("wuser");
		}), "id", "read-only");
	}

	@Test
	public void deleteUserNotVisibleUser() {
		initSpringSecurityContext("mtuyer");
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				new HashSet<>(Arrays.asList("wuser", "user1")));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected("mtuyer", "wuser")).thenThrow(
				new ValidationJsonException("id", BusinessException.KEY_UNKNOW_ID, "0", "user", "1", "mtuyer"));
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.delete("wuser");
		}), "id", BusinessException.KEY_UNKNOW_ID);
	}

	@Test
	public void resetPasswordUserNoWriteCompany() {
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
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.resetPassword("wuser");
		}), "id", "read-only");
	}

	@Test
	public void mergeUserNoChange() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
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
	public void mergeUserNoDepartment() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
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
	public void mergeUser() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
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
	public void update() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		final UserOrg user = newUser();
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
	public void updateNotWriteInCompany() {
		initSpringSecurityContext("mtuyer");
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		final UserOrg user = newUser();
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
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.update(userVo);
		}), "company", "read-only");
	}

	@Test
	public void updateFirstName() {
		// First name change only
		update2(userVo -> userVo.setFirstName("XFirst2"));
	}

	private void update2(Consumer<UserOrgEditionVo> consumer) {
		update2(consumer, c -> {
			// No change
		});
	}

	private void update2(Consumer<UserOrgEditionVo> consumerNew, Consumer<UserOrg> consumerOld) {

		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final CompanyOrg company2 = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		final GroupOrg groupOrg1 = new GroupOrg("cn=dig,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=dig rha,cn=dig as,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		final GroupOrg groupOrg3 = new GroupOrg("cn=other,dc=other,dc=com", "Other", Collections.singleton("user2"));
		final GroupOrg groupOrg4 = new GroupOrg("cn=invisible,dc=net", "Other", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		groupsMap.put("other", groupOrg3);
		groupsMap.put("invisible", groupOrg4);
		final UserOrg user = newUser(consumerOld);
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		Mockito.when(companyRepository.findById("gfi")).thenReturn(company2);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "gfi")).thenReturn(company2);
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
	public void updateLastName() {
		// Last name change only
		update2(userVo -> userVo.setLastName("XDoe2"));
	}

	@Test
	public void updateMail() {
		// Mail change only
		update2(userVo -> userVo.setMail("john31.last31@ing.com"));
	}

	@Test
	public void updateCompany() {
		update2(userVo -> userVo.setCompany("gfi"));
	}

	@Test
	public void updateUserChangeDepartment() {
		update2(userVo -> userVo.setDepartment("department2"));
	}

	@Test
	public void updateUserChangeDepartmentNotExists() {
		update2(userVo -> userVo.setDepartment("any"));
	}

	@Test
	public void updateUserNoChange() {
		update2(userVo -> {
			// No change
		});
	}

	@Test
	public void updateUserHadNoMail() {
		update2(userVo -> {
			userVo.setFirstName("XFirstA");
		}, userVo -> userVo.setMails(new ArrayList<>()));
	}

	@Test
	public void updateUserHasNoMail() {
		update2(userVo -> userVo.setMail(null));
	}

	@Test
	public void updateUserWasNotSecured() {
		update2(userVo -> {
			userVo.setFirstName("XFirstA");
		}, userVo -> userVo.setSecured(false));
	}

	@Test
	public void updateGroup() {
		// Remove group "dig"
		update2(userVo -> userVo.setGroups(Arrays.asList("other", "dig rha")),
				u -> u.setGroups(Arrays.asList("other", "dig rha", "dig")));
	}

	@Test
	public void updateGroupRemoveWithInvisible() {
		// Remove group "dig" when there is an invisible group
		update2(userVo -> userVo.setGroups(Arrays.asList("other", "dig rha")),
				u -> u.setGroups(Arrays.asList("invisible", "other", "dig rha", "dig")));
	}

	@Test
	public void findAllNotSecureByCompany() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		resource.groupResource = Mockito.mock(GroupResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));

		final List<UserOrg> data = resource.findAllNotSecure("ing", null);

		// Check the users
		checkUser(data.get(0));
	}

	@Test
	public void findAllNotSecureByVisibleCompany() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		resource.companyResource = Mockito.mock(CompanyResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.companyResource.getContainers()).thenReturn(Collections.singleton(company));
		Mockito.when(resource.companyResource.getContainersForWrite()).thenReturn(Collections.singleton(company));

		final List<UserOrg> data = resource.findAllNotSecure("ing", null);

		// Check the users
		checkUser(data.get(0));
	}

	@Test
	public void findAllNotSecureByGroup() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		resource.groupResource = Mockito.mock(GroupResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));

		final List<UserOrg> data = resource.findAllNotSecure(null, "dig");

		// Check the users
		checkUser(data.get(0));
	}

	@Test
	public void findAllNotSecureByVisibleGroup() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		resource.groupResource = Mockito.mock(GroupResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));

		final List<UserOrg> data = resource.findAllNotSecure(null, "dig");

		// Check the users
		checkUser(data.get(0));
	}

	@Test
	public void findAllNotSecure() {
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = newUser();
		users.put("wuser", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("ing");
		user2.setGroups(Collections.singletonList("any"));
		users.put("user2", user2);
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		resource.groupResource = Mockito.mock(GroupResource.class);
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing")).thenReturn(company);
		groupFindById(DEFAULT_USER, "dig", groupOrg1);
		Mockito.when(userRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(),
				ArgumentMatchers.any())).thenReturn(new PageImpl<>(new ArrayList<>(users.values())));
		Mockito.when(resource.groupResource.getContainers()).thenReturn(new HashSet<>(groupsMap.values()));
		Mockito.when(resource.groupResource.getContainersForWrite()).thenReturn(new HashSet<>(groupsMap.values()));

		final List<UserOrg> data = resource.findAllNotSecure(null, null);

		// Check the users
		checkUser(data.get(0));
	}

	@Test
	public void lock() {
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "ing");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
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
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
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
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
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
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = new UserOrg();
		user.setCompany("ing");
		user.setGroups(Collections.singleton("dig"));
		Mockito.when(userRepository.findByIdExpected(DEFAULT_USER, "wuser")).thenReturn(user);
		Mockito.when(companyRepository.findById("ing")).thenReturn(company);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		resource.unlock("wuser");
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
		mockAddUser(DEFAULT_USER);
		resource.addUserToGroup("wuser", "dig rha");
	}

	/**
	 * Add a user to a group without delegates but being system administrator.
	 */
	@Test
	public void addUserToGroupSystemAdmin() {
		initSpringSecurityContext("my-admin");
		SystemUser user = new SystemUser();
		user.setLogin("my-admin");
		em.persist(user);
		SystemRole role = new SystemRole();
		role.setName("some");
		em.persist(role);
		SystemAuthorization authorization = new SystemAuthorization();
		authorization.setType(AuthorizationType.API);
		authorization.setPattern(".*");
		authorization.setRole(role);
		em.persist(authorization);
		SystemRoleAssignment assignment = new SystemRoleAssignment();
		assignment.setRole(role);
		assignment.setUser(user);
		em.persist(assignment);
		mockAddUser("my-admin");
		resource.addUserToGroup("wuser", "dig rha");
	}

	private void mockAddUser(final String principal) {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg2);
		groupsMap.put("dig", groupOrg1);
		final UserOrg user = newUser(u -> u.setGroups(Arrays.asList("dig")));
		final CompanyOrg company = new CompanyOrg("ou=ing,ou=france,ou=people,dc=sample,dc=com", "gfi");
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
	public void addUserToGroupAlreadyMember() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		final UserOrg user = newUser();
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
	public void addUserToGroupNotWritableGroup() {
		initSpringSecurityContext("mlavoine");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final UserOrg user = new UserOrg();
		user.setCompany("gfi");
		user.setGroups(Collections.singleton("dig rha"));
		final CompanyOrg company = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		Mockito.when(companyRepository.findById("gfi")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		groupFindById("mlavoine", "dig", groupOrg1);
		groupFindById("mlavoine", "dig rha", groupOrg2);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.addUserToGroup("wuser", "dig");
		}), "group", "read-only");
	}

	/**
	 * Remove a user to a group the principal does not visible.
	 */
	@Test
	public void removeUserFromGroupNotWritableGroup() {
		initSpringSecurityContext("mlavoine");
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG",
				Collections.singleton("user1"));
		final UserOrg user = new UserOrg();
		user.setCompany("gfi");
		user.setGroups(Collections.singleton("dig rha"));
		final CompanyOrg company = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		Mockito.when(companyRepository.findById("gfi")).thenReturn(company);
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findById("wuser")).thenReturn(user);
		groupFindById("mlavoine", "dig", groupOrg1);
		groupFindById("mlavoine", "dig rha", groupOrg2);
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.removeUserFromGroup("wuser", "dig rha");
		}), "group", "read-only");
	}

	/**
	 * Test user addition to a group.
	 */
	@Test
	public void removeUserFromGroup() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com",
				"DIG RHA", Collections.singleton("wuser"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG AS",
				Collections.singleton("wuser"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig rha", groupOrg1);
		groupsMap.put("dig as", groupOrg2);
		final UserOrg user = newUser(u -> u.setGroups(Arrays.asList("dig rha", "dig as")));
		final CompanyOrg company = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
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
	public void canWriteNotSameType() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assertions.assertFalse(resource.canWrite(delegate, null, DelegateType.COMPANY));
	}

	@Test
	public void canWriteSameTypeNoRight() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assertions.assertFalse(resource.canWrite(delegate, null, DelegateType.GROUP));
	}

	@Test
	public void canWrite() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setDn("rightdn");
		delegate.setCanWrite(true);
		Assertions.assertTrue(resource.canWrite(delegate, "rightdn", DelegateType.GROUP));
	}

	/**
	 * Admin on delegate does not grant write access.
	 */
	@Test
	public void canWriteAsAdmin() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setCanAdmin(true);
		delegate.setDn("rightdn");
		Assertions.assertFalse(resource.canWrite(delegate, "rightdn", DelegateType.GROUP));
	}

	@Test
	public void canWriteAsReader() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setDn("rightdn");
		Assertions.assertFalse(resource.canWrite(delegate, "rightdn", DelegateType.GROUP));
	}

}
