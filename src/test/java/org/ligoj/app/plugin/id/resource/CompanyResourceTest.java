package org.ligoj.app.plugin.id.resource;

import java.util.Collections;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.GroupOrg;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.dao.ContainerScopeRepository;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test of {@link CompanyResource}<br>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@org.junit.FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class CompanyResourceTest extends AbstractContainerLdapResourceTest {

	private CompanyResource resource;

	@Before
	public void mock() {
		resource = new CompanyResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.iamProvider = iamProvider;
	}

	@Autowired
	private ContainerScopeRepository containerTypeLdapRepository;

	/**
	 * Check managed companies is filtered against available groups.
	 */
	@Test
	public void getContainers() {
		final TableItem<String> managed = resource.getContainers(newUriInfo());
		Assert.assertEquals(9, managed.getRecordsFiltered());
		Assert.assertEquals(9, managed.getRecordsTotal());
		Assert.assertEquals(9, managed.getData().size());
		Assert.assertEquals("external", managed.getData().get(0));
	}

	/**
	 * Check managed companies for write.
	 */
	@Test
	public void getContainersForWrite() {
		final TableItem<String> managed = resource.getContainersForWrite(newUriInfo());
		Assert.assertEquals(9, managed.getRecordsFiltered());
		Assert.assertEquals(9, managed.getRecordsTotal());
		Assert.assertEquals(9, managed.getData().size());
		Assert.assertEquals("external", managed.getData().get(0));
	}

	/**
	 * Check managed companies for write.
	 */
	@Test
	public void getContainersForWrite2() {
		initSpringSecurityContext("mtuyer");
		final TableItem<String> managed = resource.getContainersForWrite(newUriInfo());
		Assert.assertEquals(0, managed.getRecordsFiltered());
		Assert.assertEquals(0, managed.getRecordsTotal());
		Assert.assertEquals(0, managed.getData().size());
	}

	/**
	 * Check managed company is filtered against available groups for administration.
	 */
	@Test
	public void getContainersForAdmin() {
		initSpringSecurityContext("mlavoine");
		final TableItem<String> managed = resource.getContainersForAdmin(newUriInfo());
		Assert.assertEquals(0, managed.getRecordsFiltered());
		Assert.assertEquals(0, managed.getRecordsTotal());
		Assert.assertEquals(0, managed.getData().size());
	}

	@Test
	public void getContainersForAdmin2() {
		initSpringSecurityContext("fdaugan");
		final TableItem<String> managed = resource.getContainersForAdmin(newUriInfo());
		Assert.assertEquals(2, managed.getRecordsFiltered());
		Assert.assertEquals(2, managed.getRecordsTotal());
		Assert.assertEquals(2, managed.getData().size());
		Assert.assertEquals("ing", managed.getData().get(0));
		Assert.assertEquals("ing-internal", managed.getData().get(1));
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdExpectedNotExist() {
		initSpringSecurityContext("fdaugan");
		resource.findByIdExpected("any");
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdExpectedNotDelegate() {
		initSpringSecurityContext("fdaugan");
		resource.findByIdExpected("socygan");
	}

	@Test
	public void findByIdNotExists() {
		initSpringSecurityContext("fdaugan");
		Assert.assertNull(resource.findById("any"));
	}

	@Test
	public void findByIdExpected() {
		Assert.assertEquals("ou=ing,ou=external,ou=people,dc=sample,dc=com", resource.findByIdExpected("ing").getDn());
	}

	@Test
	public void findByIdExpectedMyCompany() {
		initSpringSecurityContext("mmartin");
		Assert.assertEquals("ou=gfi,ou=france,ou=people,dc=sample,dc=com", resource.findByIdExpected("gfi").getDn());
	}

	/**
	 * Check managed companies is filtered against available groups.
	 */
	@Test
	public void getContainersMyCompany() {
		initSpringSecurityContext("mmartin");
		final TableItem<String> managed = resource.getContainers(newUriInfo());
		Assert.assertEquals(4, managed.getRecordsFiltered());
		Assert.assertEquals(4, managed.getRecordsTotal());
		Assert.assertEquals(4, managed.getData().size());

		// gfi, ing, socygan
		Assert.assertEquals("gfi", managed.getData().get(0));
		Assert.assertEquals("ing", managed.getData().get(1));
		Assert.assertEquals("ing-internal", managed.getData().get(2));
		Assert.assertEquals("socygan", managed.getData().get(3));
	}

	@Test(expected = ValidationJsonException.class)
	public void createNoRight() {
		final ContainerScope typeLdap = containerTypeLdapRepository.findByName("France");
		final ContainerEditionVo group = new ContainerEditionVo();
		group.setName("New-Ax-1-z:Z 0");
		group.setType(typeLdap.getId());
		initSpringSecurityContext("mmartin");
		resource.create(group);
	}

	@Test
	public void deleteNoRight() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "unknown-id"));
		initSpringSecurityContext("mmartin");
		resource.delete("gfi");
	}

	/**
	 * Container is locked itself
	 */
	@Test
	public void deleteLocked() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "locked"));
		resource.delete("quarantine");
	}

	@Test
	public void deleteNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "unknown-id"));
		resource.delete("any-any");
	}

	@Test
	public void deleteNotEmptyLeaf() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "not-empty-company"));
		resource.delete("gfi");
	}

	@Test
	public void deleteNotEmptyParent() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "not-empty-company"));
		resource.delete("france");
	}

	@Test(expected = ValidationJsonException.class)
	public void createAlreadyExists() {
		final ContainerScope typeLdap = containerTypeLdapRepository.findByName("France");
		final ContainerEditionVo group = new ContainerEditionVo();
		group.setName("orange");
		group.setType(typeLdap.getId());
		Mockito.when(groupRepository.findById(DEFAULT_USER, "orange")).thenReturn(new GroupOrg("", "", Collections.emptySet()));
		resource.create(group);
	}

	@Test(expected = ValidationJsonException.class)
	public void createInvalidType() {
		final ContainerScope typeLdap = containerTypeLdapRepository.findByName("Fonction");
		final ContainerEditionVo company = new ContainerEditionVo();
		company.setName("New-Ax-1-z:Z 0");
		company.setType(typeLdap.getId());
		resource.create(company);
	}

	/**
	 * Create, then delete an empty company.
	 */
	@Test
	public void createDelete() {
		resource.delete("New-Ax-1-z:Z 0");
	}

	@Test
	public void findAllOnlyMyCompany() {
		Mockito.when(companyRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
		.thenReturn(new PageImpl<>(Collections.emptyList()));

		initSpringSecurityContext("mmartin");
		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAscSearch("name", "gfi"));
		Assert.assertEquals(1, groups.getRecordsTotal());
		final ContainerCountVo group0 = groups.getData().get(0);
		Assert.assertEquals("gfi", group0.getName());
		Assert.assertEquals(7, group0.getCount());
		Assert.assertEquals(7, group0.getCountVisible());
		Assert.assertFalse(group0.isCanAdmin());
		Assert.assertFalse(group0.isCanWrite());
		Assert.assertEquals("France", group0.getType());
		Assert.assertEquals("gfi", group0.getId());
		Assert.assertEquals(ContainerType.COMPANY, group0.getContainerType());
	}

	@Test
	public void findAllOnlyMyCompanyDesc() {
		Mockito.when(companyRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
		.thenReturn(new PageImpl<>(Collections.emptyList()));

		initSpringSecurityContext("mmartin");
		final UriInfo uriInfo = newUriInfo("name", "desc");
		uriInfo.getQueryParameters().add(DataTableAttributes.SEARCH, "gfi");
		final TableItem<ContainerCountVo> groups = resource.findAll(uriInfo);
		Assert.assertEquals(1, groups.getRecordsTotal());
		final ContainerCountVo group0 = groups.getData().get(0);
		Assert.assertEquals("gfi", group0.getName());
		Assert.assertEquals(7, group0.getCount());
		Assert.assertEquals(7, group0.getCountVisible());
		Assert.assertFalse(group0.isCanAdmin());
		Assert.assertFalse(group0.isCanWrite());
		Assert.assertEquals("France", group0.getType());
		Assert.assertEquals("gfi", group0.getId());
		Assert.assertEquals(ContainerType.COMPANY, group0.getContainerType());
	}

	@Test
	public void findAllExternal() {
		Mockito.when(companyRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
		.thenReturn(new PageImpl<>(Collections.emptyList()));

		initSpringSecurityContext("mtuyer");
		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAscSearch("name", "gfi"));
		Assert.assertEquals(0, groups.getRecordsTotal());
	}

	@Test
	public void findAllNoCriteriaNoType() {
		Mockito.when(companyRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
		.thenReturn(new PageImpl<>(Collections.emptyList()));

		initSpringSecurityContext("mlavoine");
		containerTypeLdapRepository.deleteAllBy("name", "Root");
		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAsc("name"));
		Assert.assertEquals(2, groups.getRecordsTotal());
		final ContainerCountVo group0 = groups.getData().get(0);
		Assert.assertEquals("ing", group0.getName());
		Assert.assertEquals(7, group0.getCount());
		Assert.assertEquals(7, group0.getCountVisible());
		Assert.assertFalse(group0.isCanAdmin());
		Assert.assertFalse(group0.isCanWrite());
		Assert.assertNull(group0.getType());
		Assert.assertEquals("ing", group0.getId());
		Assert.assertEquals(ContainerType.COMPANY, group0.getContainerType());
		Assert.assertEquals("ing-internal", groups.getData().get(1).getName());
	}

	@Test
	public void findAll() {
		Mockito.when(companyRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(new PageImpl<>(Collections.emptyList()));

		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAscSearch("name", "g"));
		Assert.assertEquals(5, groups.getRecordsTotal());
		final ContainerCountVo group0 = groups.getData().get(0);
		Assert.assertEquals("gfi", group0.getName());
		Assert.assertEquals(7, group0.getCount());
		Assert.assertEquals(7, group0.getCountVisible());
		Assert.assertTrue(group0.isCanAdmin());
		Assert.assertTrue(group0.isCanWrite());
		Assert.assertFalse(group0.isLocked());
		Assert.assertEquals("France", group0.getType());
		Assert.assertEquals("gfi", group0.getId());
		Assert.assertEquals(ContainerType.COMPANY, group0.getContainerType());

		// No group type case
		final ContainerCountVo group2 = groups.getData().get(2);
		Assert.assertEquals("ing-internal", group2.getName());
		Assert.assertEquals(1, group2.getCount());
		Assert.assertEquals(1, group2.getCountVisible());
		Assert.assertTrue(group2.isCanAdmin());
		Assert.assertTrue(group2.isCanWrite());
		Assert.assertTrue(group2.isLocked());
		Assert.assertEquals("Root", group2.getType());
		Assert.assertEquals("ing-internal", group2.getId());
		Assert.assertEquals(ContainerType.COMPANY, group2.getContainerType());
	}

	@Test
	public void findAllLocked() {
		Mockito.when(companyRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
		.thenReturn(new PageImpl<>(Collections.emptyList()));

		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAscSearch("name", "quarantine"));
		Assert.assertEquals(1, groups.getRecordsTotal());
		final ContainerCountVo group0 = groups.getData().get(0);
		Assert.assertEquals("quarantine", group0.getName());
		Assert.assertEquals(0, group0.getCount());
		Assert.assertEquals(0, group0.getCountVisible());
		Assert.assertTrue(group0.isCanAdmin());
		Assert.assertTrue(group0.isCanWrite());
		Assert.assertTrue(group0.isLocked());
		Assert.assertNull(group0.getType());
		Assert.assertEquals("quarantine", group0.getId());
		Assert.assertEquals(ContainerType.COMPANY, group0.getContainerType());
	}

	@Test
	public void isUserInternalCommpanyExternal() {
		initSpringSecurityContext("mlavoine");
		Assert.assertFalse(resource.isUserInternalCommpany());
	}

	@Test
	public void isUserInternalCommpanyAny() {
		initSpringSecurityContext("any");
		Assert.assertFalse(resource.isUserInternalCommpany());
	}

	@Test
	public void isUserInternalCommpany() {
		initSpringSecurityContext("mmartin");
		Assert.assertTrue(resource.isUserInternalCommpany());
	}

}
