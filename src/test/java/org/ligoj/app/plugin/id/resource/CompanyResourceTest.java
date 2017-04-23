package org.ligoj.app.plugin.id.resource;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.dao.ContainerScopeRepository;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.core.json.TableItem;
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
public class CompanyResourceTest extends AbstractContainerResourceTest {

	private CompanyResource resource;

	@Before
	public void mock() {
		resource = new CompanyResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		resource.iamProvider = new IamProvider[] { iamProvider };
		Mockito.when(companyRepository.getTypeName()).thenReturn("company");
	}

	@Autowired
	private ContainerScopeRepository containerScopeRepository;

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

	@Test
	public void findByIdNotExists() {
		initSpringSecurityContext("fdaugan");
		Assert.assertNull(resource.findById("any"));
	}

	@Test
	public void findByIdExpected() {
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "ing"))
				.thenReturn(new CompanyOrg("ou=ing,ou=external,ou=people,dc=sample,dc=com", "ing"));
		Assert.assertEquals("ou=ing,ou=external,ou=people,dc=sample,dc=com", resource.findByIdExpected("ing").getDn());
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

	@Test
	public void createNoRight() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("name", "already-exist"));
		final ContainerScope scope = containerScopeRepository.findByName("France");
		final ContainerEditionVo group = new ContainerEditionVo();
		group.setName("New-Ax-1-z:Z 0");
		group.setScope(scope.getId());
		initSpringSecurityContext("mmartin");
		resource.create(group);
	}

	@Test
	public void deleteNoRight() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "unknown-id"));
		initSpringSecurityContext("mmartin");
		final CompanyOrg companyOrg1 = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		Mockito.when(companyRepository.findByIdExpected("mmartin", "gfi")).thenReturn(companyOrg1);
		resource.delete("gfi");
	}

	/**
	 * Container is locked itself
	 */
	@Test
	public void deleteLocked() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "locked"));
		final CompanyOrg company = new CompanyOrg("ou=quarantine,ou=ing,ou=external,ou=people,dc=sample,dc=com", "quarantine");
		company.setLocked(true);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "quarantine")).thenReturn(company);
		resource.delete("quarantine");
	}

	@Test
	public void deleteNotEmptyParent() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", "not-empty-company"));
		final CompanyOrg companyOrg1 = new CompanyOrg("ou=france,ou=people,dc=sample,dc=com", "france");
		final CompanyOrg companyOrg2 = new CompanyOrg("ou=ing-internal,ou=ing,ou=external,ou=people,dc=sample,dc=com", "ing-internal");
		final Map<String, CompanyOrg> companies = new HashMap<>();
		companies.put("france", companyOrg1);
		companies.put("ing-internal", companyOrg2);

		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = new UserOrg();
		user1.setCompany("france");
		users.put("user1", user1);


		Mockito.when(userRepository.findAll()).thenReturn(users);
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "france")).thenReturn(companyOrg1);
		Mockito.when(companyRepository.findAll()).thenReturn(companies);
		resource.delete("france");
	}

	@Test
	public void createAlreadyExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("name", "already-exist"));
		final ContainerScope scope = containerScopeRepository.findByName("France");
		final ContainerEditionVo group = new ContainerEditionVo();
		group.setName("orange");
		group.setScope(scope.getId());
		Mockito.when(companyRepository.findById("orange")).thenReturn(new CompanyOrg("", ""));
		resource.create(group);
	}

	@Test(expected = ValidationJsonException.class)
	public void createInvalidType() {
		final ContainerScope scope = containerScopeRepository.findByName("Fonction");
		final ContainerEditionVo company = new ContainerEditionVo();
		company.setName("New-Ax-1-z:Z 0");
		company.setScope(scope.getId());
		resource.create(company);
	}

	@Test
	public void create() {
		final ContainerScope scope = containerScopeRepository.findByName("France");
		final ContainerEditionVo company = new ContainerEditionVo();
		company.setName("new-company");
		company.setScope(scope.getId());
		final CompanyOrg companyOrg1 = new CompanyOrg("ou=new-company,ou=france,ou=people,dc=sample,dc=com", "new-company");
		Mockito.when(companyRepository.create("ou=new-company,ou=france,ou=people,dc=sample,dc=com", "new-company")).thenReturn(companyOrg1);
		Assert.assertEquals("new-company", resource.create(company));
	}

	@Test
	public void delete() {
		final CompanyOrg companyOrg1 = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		Mockito.when(companyRepository.findByIdExpected(DEFAULT_USER, "gfi")).thenReturn(companyOrg1);
		resource.delete("gfi");
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
	public void findAll() {
		final CompanyOrg companyOrg1 = new CompanyOrg("ou=gfi,ou=france,ou=people,dc=sample,dc=com", "gfi");
		final CompanyOrg companyOrg2 = new CompanyOrg("ou=ing-internal,ou=ing,ou=external,ou=people,dc=sample,dc=com", "ing-internal");
		companyOrg2.setLocked(true);
		final Map<String, UserOrg> users = new HashMap<>();
		final UserOrg user1 = new UserOrg();
		user1.setCompany("france");
		users.put("user1", user1);
		final UserOrg user2 = new UserOrg();
		user2.setCompany("france");
		users.put("user2", user2);
		user2.setCompany("ing-internal");

		Mockito.when(userRepository.findAll()).thenReturn(users);
		Mockito.when(companyRepository.findAll(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
				.thenReturn(new PageImpl<>(Arrays.asList(companyOrg1, companyOrg2)));

		final TableItem<ContainerCountVo> groups = resource.findAll(newUriInfoAscSearch("name", "g"));
		Assert.assertEquals(2, groups.getRecordsTotal());
		final ContainerCountVo group0 = groups.getData().get(0);
		Assert.assertEquals("gfi", group0.getName());
		Assert.assertEquals(0, group0.getCount());
		Assert.assertEquals(0, group0.getCountVisible());
		Assert.assertFalse(group0.isCanAdmin());
		Assert.assertFalse(group0.isCanWrite());
		Assert.assertFalse(group0.isLocked());
		Assert.assertEquals("France", group0.getScope());
		Assert.assertEquals("gfi", group0.getId());
		Assert.assertEquals(ContainerType.COMPANY, group0.getContainerType());

		// No group type case
		final ContainerCountVo group2 = groups.getData().get(1);
		Assert.assertEquals("ing-internal", group2.getName());
		Assert.assertEquals(1, group2.getCount());
		Assert.assertEquals(0, group2.getCountVisible());
		Assert.assertFalse(group2.isCanAdmin());
		Assert.assertFalse(group2.isCanWrite());
		Assert.assertTrue(group2.isLocked());
		Assert.assertEquals("Root", group2.getScope());
		Assert.assertEquals("ing-internal", group2.getId());
		Assert.assertEquals(ContainerType.COMPANY, group2.getContainerType());
	}

	@Test
	public void isUserInternalCommpanyExternal() {
		initSpringSecurityContext("mlavoine");
		final UserOrg user = new UserOrg();
		user.setCompany("ext");
		final CompanyOrg company = new CompanyOrg("ou=external,dc=sample,dc=com", "sub");
		Mockito.when(userRepository.findById("mlavoine")).thenReturn(user);
		Mockito.when(companyRepository.findById("ext")).thenReturn(company);
		Mockito.when(userRepository.getPeopleInternalBaseDn()).thenReturn("ou=internal,dc=sample,dc=com");
		Assert.assertFalse(resource.isUserInternalCommpany());
	}

	@Test
	public void isUserInternalCommpanyAny() {
		initSpringSecurityContext("any");
		Mockito.when(userRepository.findById("any")).thenReturn(null);
		Mockito.when(userRepository.getPeopleInternalBaseDn()).thenReturn("ou=internal,dc=sample,dc=com");
		Assert.assertFalse(resource.isUserInternalCommpany());
	}

	@Test
	public void isUserInternalCommpany() {
		initSpringSecurityContext("mmartin");
		final UserOrg user = new UserOrg();
		user.setCompany("sub");
		final CompanyOrg company = new CompanyOrg("ou=sub,ou=internal,dc=sample,dc=com", "sub");
		Mockito.when(userRepository.findById("mmartin")).thenReturn(user);
		Mockito.when(companyRepository.findById("sub")).thenReturn(company);
		Mockito.when(userRepository.getPeopleInternalBaseDn()).thenReturn("ou=internal,dc=sample,dc=com");
		Assert.assertTrue(resource.isUserInternalCommpany());
	}

}
