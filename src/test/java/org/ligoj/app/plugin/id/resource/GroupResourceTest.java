/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.ContainerOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link GroupResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class GroupResourceTest extends AbstractContainerResourceTest {

	private GroupResource resource;

	@BeforeEach
	public void mock() {
		resource = new GroupResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.iamProvider = new IamProvider[] { iamProvider };
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
		Mockito.when(
				groupRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(new PageImpl<>(Arrays.asList(groupOrg1, groupOrg2)));

		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAscSearch("name", "d"));
		Assertions.assertEquals(2, groups.getRecordsTotal());
		Assertions.assertEquals(2, groups.getRecordsFiltered());
		Assertions.assertEquals(2, groups.getData().size());

		final ContainerCountVo group0 = groups.getData().get(0);
		Assertions.assertEquals("DIG", group0.getName());
		Assertions.assertEquals(1, group0.getCount());
		Assertions.assertEquals(0, group0.getCountVisible());
		Assertions.assertTrue(group0.isCanAdmin());
		Assertions.assertTrue(group0.isCanWrite());
		Assertions.assertEquals("Fonction", group0.getScope());
		Assertions.assertEquals("dig", group0.getId());
		Assertions.assertFalse(group0.isLocked());

		final ContainerCountVo group10 = groups.getData().get(1);
		Assertions.assertEquals("DIG RHA", group10.getName());
		Assertions.assertEquals(1, group10.getCount());
		Assertions.assertEquals(0, group10.getCountVisible());
		Assertions.assertTrue(group10.isCanAdmin());
		Assertions.assertTrue(group10.isCanWrite());
		Assertions.assertEquals("Fonction", group10.getScope());
		Assertions.assertEquals(ContainerType.GROUP, group10.getContainerType());
		Assertions.assertTrue(group10.isLocked());

	}

	@Test
	public void findByNameNoType() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "business solution"))
				.thenReturn(new GroupOrg("cn=Business Solution,ou=groups,dc=sample,dc=com", "Business Solution", null));
		final ContainerWithScopeVo group = resource.findByName("business solution");
		Assertions.assertEquals("Business Solution", group.getName());
		Assertions.assertNull(group.getScope());
	}

	@Test
	public void findByNameNotExistingGroup() {
		Assertions.assertNull(resource.findByName("any"));
	}

	@Test
	public void findByName() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "dig as"))
				.thenReturn(new GroupOrg("cn=DIG AS,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "DIG AS", null));
		final ContainerWithScopeVo group = resource.findByName("dig as");
		Assertions.assertEquals("DIG AS", group.getName());
		Assertions.assertEquals("Fonction", group.getScope());
	}

	@Test
	public void findByIdNotExists() {
		Assertions.assertNull(resource.findById("any"));
	}

	/**
	 * There is a delegate of "business solution" for this user, but the user
	 * does not exist anymore.
	 */
	@Test
	public void findByIdUserNoRight() {
		initSpringSecurityContext("assist");
		Assertions.assertNull(resource.findById("business solution"));
	}

	@Test
	public void exists() {
		Mockito.when(groupRepository.findById(DEFAULT_USER, "dig as"))
				.thenReturn(new GroupOrg("cn=DIG AS,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig as", null));
		Assertions.assertTrue(resource.exists("dig as"));
	}

	@Test
	public void existsNot() {
		Assertions.assertFalse(resource.exists("any"));
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
		group.setScope(scope.getId());
		final GroupOrg groupOrg1 = new GroupOrg("cn=new-group", "new-group", null);
		Mockito.when(groupRepository.create(expected, "new-group")).thenReturn(groupOrg1);
		Assertions.assertEquals("new-group", resource.create(group));
	}

	@Test
	public void deleteNoRight() {
		initSpringSecurityContext("mmartin");
		Mockito.when(groupRepository.findByIdExpected("mmartin", "dig rha"))
				.thenReturn(new GroupOrg("cn=DIG RHA,cn=DIG AS,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", "dig rha", null));
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.delete("dig rha");
		}), "group", "unknown-id");
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
		Assertions.assertEquals("cn=new-group,cn=DIG,ou=fonction,ou=groups,dc=sample,dc=com", resource.toDn(group, scope));
	}

	@Test
	public void toDnParentInvalid() {
		final ContainerScope scope = containerScopeRepository.findByName("Fonction");
		final GroupEditionVo group = new GroupEditionVo();
		group.setName("new-group");
		group.setParent(" DiG ");
		Mockito.when(groupRepository.findByIdExpected(DEFAULT_USER, "dig"))
				.thenReturn(new GroupOrg("cn=ext,dc=sample,dc=com", "DIG", null));
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.toDn(group, scope);
		}), "parent", "container-parent-type-match");
	}

	@Test
	public void toDn() {
		final ContainerScope scope = containerScopeRepository.findByName("Fonction");
		final GroupEditionVo group = new GroupEditionVo();
		group.setName("new-group");
		Assertions.assertEquals("cn=new-group,ou=fonction,ou=groups,dc=sample,dc=com", resource.toDn(group, scope));
	}

	@Test
	public void emptyNoRight() {
		initSpringSecurityContext("mmartin");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.empty("dig rha");
		}), "group", "unknown-id");
	}

	/**
	 * Check visible group is filtered against available groups for write.
	 */
	@Test
	public void getContainersForWrite() {
		initSpringSecurityContext("mlavoine");
		final TableItem<String> items = resource.getContainersForWrite(newUriInfo());
		Assertions.assertEquals(0, items.getRecordsFiltered());
		Assertions.assertEquals(0, items.getRecordsTotal());
		Assertions.assertEquals(0, items.getData().size());
	}

	/**
	 * Check visible group is filtered against available groups for
	 * administration.
	 */
	@Test
	public void getContainersForAdmin() {
		initSpringSecurityContext("mtuyer");
		final TableItem<String> items = resource.getContainersForAdmin(newUriInfo());

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
	public void getContainersForAdminNoRight() {
		initSpringSecurityContext("mlavoine");
		final TableItem<String> items = resource.getContainersForAdmin(newUriInfo());
		Assertions.assertEquals(0, items.getRecordsFiltered());
		Assertions.assertEquals(0, items.getRecordsTotal());
		Assertions.assertEquals(0, items.getData().size());
	}

	@Test
	public void getContainersDelegateTreeExactParentDn() {
		initSpringSecurityContext("mlavoine");
		final TableItem<String> items = resource.getContainers(newUriInfo());
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
	public void getContainersDelegateTreeSubParentDn() {
		initSpringSecurityContext("mtuyer");
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		final TableItem<String> items = resource.getContainers(uriInfo);
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
	public void getContainersNoDelegate() {
		initSpringSecurityContext("any");
		final TableItem<String> items = resource.getContainers(newUriInfo());
		Assertions.assertEquals(0, items.getRecordsFiltered());
		Assertions.assertEquals(0, items.getRecordsTotal());
		Assertions.assertEquals(0, items.getData().size());
	}

	@Test
	public void getContainersDelegateGroup() {
		initSpringSecurityContext("someone");
		final TableItem<String> items = resource.getContainers(newUriInfo());
		Assertions.assertEquals(1, items.getRecordsFiltered());
		Assertions.assertEquals(1, items.getRecordsTotal());
		Assertions.assertEquals(1, items.getData().size());
		Assertions.assertTrue(items.getData().contains("DIG RHA"));
	}

	@Test
	public void newContainerCountVo() {
		ContainerOrg rawContainer = new ContainerOrg("dn=no-scope", "name");
		Assertions.assertNull(resource
				.newContainerCountVo(rawContainer, Collections.emptySet(), Collections.emptySet(), Collections.emptyList()).getScope());
	}
}
