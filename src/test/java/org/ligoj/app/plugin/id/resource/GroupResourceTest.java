/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.*;
import org.ligoj.app.model.ContainerType;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.*;

/**
 * Test class of {@link GroupResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class GroupResourceTest extends AbstractContainerResourceTest {

	public static final String GROUP_FUNCTION = "Fonction";
	private GroupResource resource;

	@BeforeEach
	void mock() {
		resource = new GroupResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.iamProvider = new IamProvider[]{iamProvider};
		Mockito.when(groupRepository.getTypeName()).thenReturn("group");
	}

	@Test
	void findAll() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.singleton("user1"));
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.singleton("user2"));
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);

		final var users = new HashMap<String, UserOrg>();
		final var user1 = new UserOrg();
		user1.setCompany("france");
		users.put("user1", user1);
		final var user2 = new UserOrg();
		user2.setCompany("france");
		users.put("user2", user2);
		user2.setCompany("ing-internal");

		final var companyOrg1 = new CompanyOrg("ou=france,ou=people,dc=sample,dc=com", "france");
		final var companyOrg2 = new CompanyOrg("ou=ing-internal,ou=ing,ou=external,ou=people,dc=sample,dc=com", "ing-internal");
		final var companies = new HashMap<String, CompanyOrg>();
		companies.put("france", companyOrg1);
		companies.put("ing-internal", companyOrg2);

		Mockito.when(companyRepository.findAll()).thenReturn(companies);
		Mockito.when(userRepository.findAll()).thenReturn(users);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(
						groupRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(new PageImpl<>(Arrays.asList(groupOrg1, groupOrg2)));

		final var groups = resource.findAll(newUriInfoAscSearch("name", "d"));
		Assertions.assertEquals(2, groups.getRecordsTotal());
		Assertions.assertEquals(2, groups.getRecordsFiltered());
		Assertions.assertEquals(2, groups.getData().size());

		final var group0 = groups.getData().get(0);
		Assertions.assertEquals("DIG", group0.getName());
		Assertions.assertEquals(1, group0.getCount());
		Assertions.assertEquals(0, group0.getCountVisible());
		Assertions.assertTrue(group0.isCanAdmin());
		Assertions.assertTrue(group0.isCanWrite());
		Assertions.assertEquals(GROUP_FUNCTION, group0.getScope());
		Assertions.assertEquals("dig", group0.getId());
		Assertions.assertFalse(group0.isLocked());

		final var group10 = groups.getData().get(1);
		Assertions.assertEquals("DIG RHA", group10.getName());
		Assertions.assertEquals(1, group10.getCount());
		Assertions.assertEquals(0, group10.getCountVisible());
		Assertions.assertTrue(group10.isCanAdmin());
		Assertions.assertTrue(group10.isCanWrite());
		Assertions.assertEquals(GROUP_FUNCTION, group10.getScope());
		Assertions.assertEquals(ContainerType.GROUP, group10.getContainerType());
		Assertions.assertTrue(group10.isLocked());
		Assertions.assertNull(group10.getParents());
	}

	@Test
	void findByNameNoType() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "business solution"))
				.thenReturn(new GroupOrg("cn=Business Solution,ou=groups,dc=sample,dc=com", "Business Solution", null));
		final var group = resource.findByName("business solution");
		Assertions.assertEquals("Business Solution", group.getName());
		Assertions.assertNull(group.getScope());
	}

	@Test
	void findByNameNotExistingGroup() {
		Assertions.assertNull(resource.findByName("any"));
	}

	@Test
	void findByName() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "dig as"))
				.thenReturn(new GroupOrg("cn=DIG AS,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG AS", null));
		final var group = resource.findByName("dig as");
		Assertions.assertEquals("DIG AS", group.getName());
		Assertions.assertEquals(GROUP_FUNCTION, group.getScope());
	}

	@Test
	void findByIdNotExists() {
		Assertions.assertNull(resource.findById("any"));
	}

	/**
	 * There is a delegate of "business solution" for this user, but the user
	 * does not exist anymore.
	 */
	@Test
	void findByIdUserNoRight() {
		initSpringSecurityContext("assist");
		Assertions.assertNull(resource.findById("business solution"));
	}

	@Test
	void exists() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "dig as"))
				.thenReturn(new GroupOrg("cn=DIG AS,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig as", null));
		Assertions.assertTrue(resource.exists("dig as"));
	}

	@Test
	void existsNot() {
		Assertions.assertFalse(resource.exists("any"));
	}

	@Test
	void createWithParent() {
		final var group = new GroupEditionVo();
		group.setDepartments(Collections.singletonList("SOME"));
		group.setOwners(Collections.singletonList("fdaugan"));
		group.setAssistants(Collections.singletonList("wuser"));
		group.setParent("DIG");

		final var user = new UserOrg();
		user.setCompany("ext");
		user.setDn("uid=wuser");
		final var user2 = new UserOrg();
		user2.setCompany("internal");
		user2.setDn("uid=fdaugan");
		Mockito.when(userRepository.findByIdExpected("wuser")).thenReturn(user);
		Mockito.when(userRepository.findByIdExpected("fdaugan")).thenReturn(user2);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig"))
				.thenReturn(new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", null));

		createInternal(group, "cn=new-group,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com");
	}

	@Test
	void create() {
		createInternal(new GroupEditionVo(), "cn=new-group,ou=fonction,ou=groups,dc=sample,dc=com");
	}

	private void createInternal(final GroupEditionVo group, final String expected) {
		final var scope = containerScopeRepository.findByName(GROUP_FUNCTION);
		group.setName("new-group");
		group.setScope(scope.getId());
		final var groupOrg1 = new GroupOrg("cn=new-group", "new-group", null);
		Mockito.when(groupRepository.create(expected, "new-group")).thenReturn(groupOrg1);
		Assertions.assertEquals("new-group", resource.create(group));
	}

	@Test
	void deleteNoRight() {
		initSpringSecurityContext("mmartin");
		Mockito.when(groupRepository.findByIdExpected("mmartin", "dig rha"))
				.thenReturn(new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig rha", null));
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.delete("dig rha")), "group", "unknown-id");
	}

	@Test
	void empty() {
		final var groupOrg1 = new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", Collections.emptySet());
		final var groupOrg2 = new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG RHA",
				Collections.emptySet());
		groupOrg2.setLocked(true);
		final var groupsMap = new HashMap<String, GroupOrg>();
		groupsMap.put("dig", groupOrg1);
		groupsMap.put("dig rha", groupOrg2);
		Mockito.when(groupRepository.findAll()).thenReturn(groupsMap);
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig rha"))
				.thenReturn(new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig rha", null));
		resource.empty("dig rha");
	}

	@Test
	void toDnParent() {
		final var scope = containerScopeRepository.findByName(GROUP_FUNCTION);
		final var group = new GroupEditionVo();
		group.setName("new-group");
		group.setParent(" DiG ");
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig"))
				.thenReturn(new GroupOrg("cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG", null));
		Assertions.assertEquals("cn=new-group,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", resource.toDn(group, scope));
	}

	@Test
	void toDnParentInvalid() {
		final var scope = containerScopeRepository.findByName(GROUP_FUNCTION);
		final var group = new GroupEditionVo();
		group.setName("new-group");
		group.setParent(" DiG ");
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig"))
				.thenReturn(new GroupOrg("cn=ext,dc=sample,dc=com", "DIG", null));
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.toDn(group, scope)), "parent", "container-parent-type-match");
	}

	@Test
	void toDn() {
		final var scope = containerScopeRepository.findByName(GROUP_FUNCTION);
		final var group = new GroupEditionVo();
		group.setName("new-group");
		Assertions.assertEquals("cn=new-group,ou=fonction,ou=groups,dc=sample,dc=com", resource.toDn(group, scope));
	}

	@Test
	void emptyNoRight() {
		initSpringSecurityContext("mmartin");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> resource.empty("dig rha")), "group", "unknown-id");
	}

	/**
	 * Check visible group is filtered against available groups for write.
	 */
	@Test
	void getContainersForWrite() {
		initSpringSecurityContext("mlavoine");
		final var items = resource.getContainersForWrite(newUriInfo());
		Assertions.assertEquals(0, items.getRecordsFiltered());
		Assertions.assertEquals(0, items.getRecordsTotal());
		Assertions.assertEquals(0, items.getData().size());
	}

	/**
	 * Check visible group is filtered against available groups for
	 * administration.
	 */
	@Test
	void getContainersForAdmin() {
		initSpringSecurityContext("mtuyer");
		final var items = resource.getContainersForAdmin(newUriInfo());

		// This user can see 4 groups from the direct admin delegates to him
		Assertions.assertEquals(4, items.getRecordsFiltered());
		Assertions.assertEquals(4, items.getRecordsTotal());
		Assertions.assertEquals(4, items.getData().size());
	}

	/**
	 * Check visible group is filtered against available groups for
	 * administration.
	 */
	@Test
	void getContainersForAdminNoRight() {
		initSpringSecurityContext("mlavoine");
		final var items = resource.getContainersForAdmin(newUriInfo());
		Assertions.assertEquals(0, items.getRecordsFiltered());
		Assertions.assertEquals(0, items.getRecordsTotal());
		Assertions.assertEquals(0, items.getData().size());
	}

	@Test
	void getContainersDelegateTreeExactParentDn() {
		initSpringSecurityContext("mlavoine");
		final var items = resource.getContainers(newUriInfo());
		Assertions.assertEquals(4, items.getRecordsFiltered());
		Assertions.assertEquals(4, items.getRecordsTotal());
		Assertions.assertEquals(4, items.getData().size());

		// Brought by a delegate of "cn=biz agency,ou=tools,dc=sample,dc=com" to
		// company user "mlavoine"
		Assertions.assertTrue(items.getData().contains("Biz Agency"));
		Assertions.assertTrue(items.getData().contains("Biz Agency Manager"));

		// Brought by a delegate of "Business Solution" to company "ing"
		Assertions.assertTrue(items.getData().contains("Business Solution"));
		Assertions.assertTrue(items.getData().contains("Sub Business Solution"));
	}

	@Test
	void getContainersDelegateTreeSubParentDn() {
		initSpringSecurityContext("mtuyer");
		final var uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		final var items = resource.getContainers(uriInfo);
		Assertions.assertEquals(6, items.getRecordsFiltered());
		Assertions.assertEquals(6, items.getRecordsTotal());
		Assertions.assertEquals(6, items.getData().size());

		// Brought by a delegate of "ou=fonction,ou=groups,dc=sample,dc=com" to
		// company user "mtuyer"
		Assertions.assertTrue(items.getData().contains("DIG AS"));
		Assertions.assertTrue(items.getData().contains("DIG"));

		// Brought by a delegate of "Business Solution" to company "ing"
		Assertions.assertTrue(items.getData().contains("Business Solution"));
		Assertions.assertTrue(items.getData().contains("Sub Business Solution"));
	}

	@Test
	void getContainersNoDelegate() {
		initSpringSecurityContext("any");
		final var items = resource.getContainers(newUriInfo());
		Assertions.assertEquals(0, items.getRecordsFiltered());
		Assertions.assertEquals(0, items.getRecordsTotal());
		Assertions.assertEquals(0, items.getData().size());
	}

	@Test
	void getContainersDelegateGroup() {
		initSpringSecurityContext("someone");
		final var items = resource.getContainers(newUriInfo());
		Assertions.assertEquals(1, items.getRecordsFiltered());
		Assertions.assertEquals(1, items.getRecordsTotal());
		Assertions.assertEquals(1, items.getData().size());
		Assertions.assertTrue(items.getData().contains("DIG RHA"));
	}

	@Test
	void newContainerCountVo() {
		final var rawContainer = new GroupOrg("dn=no-scope", "name", Collections.emptySet());
		final var container = new ContainerCountVo();
		resource.fillContainerCountVo(rawContainer, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), container, Collections.emptyMap());
		Assertions.assertNull(container.getScope());
		Assertions.assertNull(container.getParents());
	}

	@Test
	void newContainerCountVoWithParents() {
		final var containerVo = new ContainerCountVo();
		final var container = new GroupOrg("dn=no-scope", "name", Collections.emptySet());
		final var parentContainer = new GroupOrg("dn=no-scope", "direct-parent", Collections.emptySet());
		final var rootContainer = new GroupOrg("dn=no-scope", "root", Collections.emptySet());
		container.setParent("direct-parent");
		parentContainer.setParent("root");
		final var all = Map.of("direct-parent", parentContainer, "root", rootContainer, "name", container);
		resource.fillContainerCountVo(container, Collections.emptySet(), Collections.emptySet(), Collections.emptyList(), containerVo, all);
		Assertions.assertNull(containerVo.getScope());
		Assertions.assertEquals(List.of("direct-parent", "root"), containerVo.getParents());
	}
}