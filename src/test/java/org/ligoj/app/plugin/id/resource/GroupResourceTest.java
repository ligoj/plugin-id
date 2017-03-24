package org.ligoj.app.plugin.id.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.CompanyOrg;
import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.api.UserOrg;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link GroupResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@org.junit.FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class GroupResourceTest extends AbstractContainerResourceTest {

	private GroupResource resource;

	@Before
	public void mock() {
		resource = new GroupResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.iamProvider = iamProvider;
		Mockito.when(groupRepository.getTypeName()).thenReturn("group");
	}

	@Test
	public void findAll() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("user1"));
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);

		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = new UserOrg();
		user1.setCompany("france");
		users.put("user1", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("france");
		users.put("user2", user2);
		user2.setCompany("ing-internal");

		final CompanyOrg companyOrg1 = new CompanyOrg("ou=france,ou=people,dc=sample,dc=com", "france");
		final CompanyOrg companyOrg2 = new CompanyOrg("ou=ing-internal,ou=ing,ou=external,ou=people,dc=sample,dc=com", "ing-internal");
		final Map<String, CompanyOrg> companies = new HashMap<>();
		companies.put("france", companyOrg1);
		companies.put("ing-internal", companyOrg2);

		Mockito.when(companyRepository.findAll()).thenReturn(companies);
		Mockito.when(userRepository.findAll()).thenReturn(users);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(groupRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(new PageImpl<>(Arrays.asList(groupOrg1, groupOrg2)));

		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAscSearch("name", "d"));
		Assert.assertEquals(2, groups.getRecordsTotal());
		Assert.assertEquals(2, groups.getRecordsFiltered());
		Assert.assertEquals(2, groups.getData().size());

		final ContainerCountVo group0 = groups.getData().get(0);
		Assert.assertEquals("DIG", group0.getName());
		Assert.assertEquals(1, group0.getCount());
		Assert.assertEquals(0, group0.getCountVisible());
		Assert.assertTrue(group0.isCanAdmin());
		Assert.assertTrue(group0.isCanWrite());
		Assert.assertEquals("Fonction", group0.getType());
		Assert.assertEquals("dig", group0.getId());
		Assert.assertFalse(group0.isLocked());

		final ContainerCountVo group10 = groups.getData().get(1);
		Assert.assertEquals("DIG RHA", group10.getName());
		Assert.assertEquals(1, group10.getCount());
		Assert.assertEquals(0, group10.getCountVisible());
		Assert.assertTrue(group10.isCanAdmin());
		Assert.assertTrue(group10.isCanWrite());
		Assert.assertEquals("Fonction", group10.getType());
		Assert.assertEquals(ContainerType.GROUP, group10.getContainerType());
		Assert.assertTrue(group10.isLocked());

	}

	@Test
	public void findByNameNoType() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "business solution"))
				.thenReturn(new GroupOrg("cn=Business Solution,ou=groups,dc=sample,dc=com", "Business Solution", null));
		final ContainerWithTypeVo group = resource.findByName("business solution");
		Assert.assertEquals("Business Solution", group.getName());
		Assert.assertNull(group.getType());
	}

	@Test
	public void findByNameNotExistingGroup() {
		Assert.assertNull(resource.findByName("any"));
	}

	@Test
	public void findByName() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "dig as"))
				.thenReturn(new GroupOrg("cn=DIG AS,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG AS", null));
		final ContainerWithTypeVo group = resource.findByName("dig as");
		Assert.assertEquals("DIG AS", group.getName());
		Assert.assertEquals("Fonction", group.getType());
	}

	@Test
	public void findByIdNotExists() {
		Assert.assertNull(resource.findById("any"));
	}

	/**
	 * There is a delegate of "business solution" for this user, but the user does not exist anymore.
	 */
	@Test
	public void findByIdUserNoRight() {
		initSpringSecurityContext("assist");
		Assert.assertNull(resource.findById("business solution"));
	}

	@Test
	public void exists() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "dig as"))
				.thenReturn(new GroupOrg("cn=DIG AS,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig as", null));
		Assert.assertTrue(resource.exists("dig as"));
	}

	@Test
	public void existsNot() {
		Assert.assertFalse(resource.exists("any"));
	}

	@Test
	public void createWithParent() {
		final GroupEditionVo group = new GroupEditionVo();
		group.setDepartments(Collections.singletonList("SOME"));
		group.setOwners(Collections.singletonList("fdaugan"));
		group.setAssistants(Collections.singletonList("wuser"));
		group.setParent("DIG");

		final UserOrg user = new UserOrg();
		user.setCompany("ext");
		user.setDn("uid=wuser");
		final UserOrg user2 = new UserOrg();
		user2.setCompany("internal");
		user2.setDn("uid=fdaugan");
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findByIdExpected("fdaugan")).thenReturn(user2);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig"))
				.thenReturn(new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", null));

		createInternal(group, "cn=new-group,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com");
	}

	@Test
	public void create() {
		createInternal(new GroupEditionVo(), "cn=new-group,ou=fonction,ou=groups,dc=sample,dc=com");
	}

	private void createInternal(final GroupEditionVo group, final String expected) {
		final ContainerScope scope = containerScopeRepository.findByName("Fonction");
		group.setName("new-group");
		group.setType(scope.getId());
		final GroupOrg groupOrg1 = new GroupOrg("cn=new-group", "new-group", null);
		Mockito.when(groupRepository.create(expected, "new-group")).thenReturn(groupOrg1);
		Assert.assertEquals("new-group", resource.create(group));
	}

	@Test
	public void deleteNoRight() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("group", "unknown-id"));
		initSpringSecurityContext("mmartin");
		Mockito.when(groupRepository.findByIdExpected("mmartin", "dig rha"))
				.thenReturn(new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig rha", null));
		resource.delete("dig rha");
	}

	@Test
	public void empty() {
		final GroupOrg groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.emptySet());
		final GroupOrg groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.emptySet());
		groupOrg2.setLocked(true);
		final Map<String, GroupOrg> groupsMap = new HashMap<>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig rha"))
				.thenReturn(new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig rha", null));
		resource.empty("dig rha");
	}

	@Test
	public void toDnParent() {
		final ContainerScope scope = containerScopeRepository.findByName("Fonction");
		final GroupEditionVo group = new GroupEditionVo();
		group.setName("new-group");
		group.setParent(" DiG ");
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig"))
				.thenReturn(new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", null));
		Assert.assertEquals("cn=new-group,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", resource.toDn(group, scope));
	}

	@Test
	public void toDnParentInvalid() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("parent", "container-parent-type-match"));
		final ContainerScope scope = containerScopeRepository.findByName("Fonction");
		final GroupEditionVo group = new GroupEditionVo();
		group.setName("new-group");
		group.setParent(" DiG ");
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig")).thenReturn(new GroupOrg("cn=ext,dc=sample,dc=com", "DIG", null));
		Assert.assertEquals("cn=new-group", resource.toDn(group, scope));
	}

	@Test
	public void toDn() {
		final ContainerScope scope = containerScopeRepository.findByName("Fonction");
		final GroupEditionVo group = new GroupEditionVo();
		group.setName("new-group");
		Assert.assertEquals("cn=new-group,ou=fonction,ou=groups,dc=sample,dc=com", resource.toDn(group, scope));
	}

	@Test(expected = ValidationJsonException.class)
	public void emptyNoRight() {
		initSpringSecurityContext("mmartin");
		resource.empty("dig rha");
	}

	/**
	 * Check managed group is filtered against available groups for write.
	 */
	@Test
	public void getContainersForWrite() {
		initSpringSecurityContext("mlavoine");
		final TableItem<String> managed = resource.getContainersForWrite(newUriInfo());
		Assert.assertEquals(0, managed.getRecordsFiltered());
		Assert.assertEquals(0, managed.getRecordsTotal());
		Assert.assertEquals(0, managed.getData().size());
	}

	/**
	 * Check managed group is filtered against available groups for administration.
	 */
	@Test
	public void getContainersForAdmin() {
		initSpringSecurityContext("mtuyer");
		final TableItem<String> managed = resource.getContainersForAdmin(newUriInfo());

		// This user can see 4 groups from the direct admin delegates to him
		Assert.assertEquals(4, managed.getRecordsFiltered());
		Assert.assertEquals(4, managed.getRecordsTotal());
		Assert.assertEquals(4, managed.getData().size());
	}

	/**
	 * Check managed group is filtered against available groups for administration.
	 */
	@Test
	public void getContainersForAdminNoRight() {
		initSpringSecurityContext("mlavoine");
		final TableItem<String> managed = resource.getContainersForAdmin(newUriInfo());
		Assert.assertEquals(0, managed.getRecordsFiltered());
		Assert.assertEquals(0, managed.getRecordsTotal());
		Assert.assertEquals(0, managed.getData().size());
	}

	@Test
	public void getContainersDelegateTreeExactParentDn() {
		initSpringSecurityContext("mlavoine");
		final TableItem<String> managed = resource.getContainers(newUriInfo());
		Assert.assertEquals(4, managed.getRecordsFiltered());
		Assert.assertEquals(4, managed.getRecordsTotal());
		Assert.assertEquals(4, managed.getData().size());

		// Brought by a delegate of "cn=biz agency,ou=tools,dc=sample,dc=com" to company user "mlavoine"
		Assert.assertTrue(managed.getData().contains("Biz Agency"));
		Assert.assertTrue(managed.getData().contains("Biz Agency Manager"));

		// Brought by a delegate of "Business Solution" to company "ing"
		Assert.assertTrue(managed.getData().contains("Business Solution"));
		Assert.assertTrue(managed.getData().contains("Sub Business Solution"));
	}

	@Test
	public void getContainersDelegateTreeSubParentDn() {
		initSpringSecurityContext("mtuyer");
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		final TableItem<String> managed = resource.getContainers(uriInfo);
		Assert.assertEquals(6, managed.getRecordsFiltered());
		Assert.assertEquals(6, managed.getRecordsTotal());
		Assert.assertEquals(6, managed.getData().size());

		// Brought by a delegate of "ou=fonction,ou=groups,dc=sample,dc=com" to company user "mtuyer"
		Assert.assertTrue(managed.getData().contains("DIG AS"));
		Assert.assertTrue(managed.getData().contains("DIG"));

		// Brought by a delegate of "Business Solution" to company "ing"
		Assert.assertTrue(managed.getData().contains("Business Solution"));
		Assert.assertTrue(managed.getData().contains("Sub Business Solution"));
	}

	@Test
	public void getContainersNoDelegate() {
		initSpringSecurityContext("any");
		final TableItem<String> managed = resource.getContainers(newUriInfo());
		Assert.assertEquals(0, managed.getRecordsFiltered());
		Assert.assertEquals(0, managed.getRecordsTotal());
		Assert.assertEquals(0, managed.getData().size());
	}

	@Test
	public void getContainersDelegateGroup() {
		initSpringSecurityContext("someone");
		final TableItem<String> managed = resource.getContainers(newUriInfo());
		Assert.assertEquals(1, managed.getRecordsFiltered());
		Assert.assertEquals(1, managed.getRecordsTotal());
		Assert.assertEquals(1, managed.getData().size());
		Assert.assertTrue(managed.getData().contains("DIG RHA"));
	}

}
