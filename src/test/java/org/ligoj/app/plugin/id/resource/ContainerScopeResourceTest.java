/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.util.List;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.core.UriInfo;

import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.dao.ContainerScopeRepository;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.AbstractJpaTest;
import org.ligoj.bootstrap.core.json.TableItem;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.orm.jpa.JpaObjectRetrievalFailureException;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link ContainerScopeResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class ContainerScopeResourceTest extends AbstractJpaTest {

	@Autowired
	private ContainerScopeResource resource;

	@Autowired
	private ContainerScopeRepository repository;

	@BeforeEach
	void setUpEntities() throws IOException {
		persistEntities("csv", new Class[] { ContainerScope.class }, "UTF-8");
	}

	@Test
	void findAll() {
		// Create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();

		final TableItem<ContainerScope> result = resource.findAll(ContainerType.GROUP, uriInfo);
		Assertions.assertEquals(4, result.getData().size());

		final ContainerScope type = result.getData().get(1);
		checkType(type);
	}

	@Test
	void findAll2() {
		final List<ContainerScope> result = resource.findAllDescOrder(ContainerType.GROUP);
		Assertions.assertEquals(4, result.size());
		final ContainerScope type = result.get(2);
		Assertions.assertEquals("Project", type.getName());
		Assertions.assertEquals("ou=projects,dc=sample,dc=com", type.getDn());
	}

	@Test
	void findAllCompany() {
		final List<ContainerScope> result = resource.findAllDescOrder(ContainerType.COMPANY);
		Assertions.assertEquals(2, result.size());
		final ContainerScope type = result.get(0);
		Assertions.assertEquals("France", type.getName());
		Assertions.assertEquals("ou=france,ou=people,dc=sample,dc=com", type.getDn());
		Assertions.assertEquals(ContainerType.COMPANY, type.getType());
	}

	@Test
	void findAllGlobalSearch() {
		// Create a mock URI info with pagination information
		final UriInfo uriInfo = newFindAllParameters();

		// Add criteria
		uriInfo.getQueryParameters().add("q", "j");

		final TableItem<ContainerScope> result = resource.findAll(ContainerType.GROUP, uriInfo);
		Assertions.assertEquals(1, result.getData().size());

		final ContainerScope type = result.getData().get(0);
		checkType(type);
	}

	private UriInfo newFindAllParameters() {
		final UriInfo uriInfo = Mockito.mock(UriInfo.class);
		Mockito.when(uriInfo.getQueryParameters()).thenReturn(new MetadataMap<>());
		uriInfo.getQueryParameters().add("draw", "1");
		uriInfo.getQueryParameters().add("start", "0");
		uriInfo.getQueryParameters().add("length", "10");
		uriInfo.getQueryParameters().add("columns[0][data]", "name");
		uriInfo.getQueryParameters().add("order[0][column]", "0");
		uriInfo.getQueryParameters().add("order[0][dir]", "desc");
		return uriInfo;
	}

	/**
	 * test {@link ContainerScopeResource#findById(int)}
	 */
	@Test
	void findByIdInvalid() {
		Assertions.assertThrows(JpaObjectRetrievalFailureException.class, () -> resource.findById(0));
	}

	/**
	 * test {@link ContainerScopeResource#findById(int)}
	 */
	@Test
	void findById() {
		final Integer id = repository.findAll(Sort.by("name")).get(3).getId();
		checkType(resource.findById(id));
	}

	@Test
	void findByName() {
		checkType(resource.findByName("Project"));
	}

	private void checkType(final ContainerScope type) {
		Assertions.assertEquals("Project", type.getName());
		Assertions.assertTrue(type.isLocked());
		Assertions.assertEquals("ou=projects,dc=sample,dc=com", type.getDn());
		Assertions.assertEquals(ContainerType.GROUP, type.getType());
	}

	/**
	 * test create
	 */
	@Test
	void create() {
		final ContainerScope vo = new ContainerScope();
		vo.setName("Name");
		vo.setDn("dc=sample,dc=com");
		vo.setType(ContainerType.GROUP);
		final int id = resource.create(vo);
		em.flush();
		em.clear();

		final ContainerScope entity = repository.findOneExpected(id);
		Assertions.assertEquals("Name", entity.getName());
		Assertions.assertEquals("dc=sample,dc=com", entity.getDn());
		Assertions.assertFalse(entity.isLocked());
		Assertions.assertEquals(id, entity.getId().intValue());
	}

	/**
	 * test create duplicate DN
	 */
	@Test
	void createDuplicateDn() {
		final ContainerScope vo = new ContainerScope();
		vo.setName("Name");
		vo.setType(ContainerType.GROUP);
		vo.setDn("ou=projects,dc=sample,dc=com");
		Assertions.assertThrows(DataIntegrityViolationException.class, () -> resource.create(vo));
	}

	/**
	 * test create duplicate name
	 */
	@Test
	void createDuplicateName() {
		final ContainerScope vo = new ContainerScope();
		vo.setName("Project");
		vo.setDn("dc=sample,dc=com");
		vo.setType(ContainerType.GROUP);
		Assertions.assertThrows(DataIntegrityViolationException.class, () -> resource.create(vo));
	}

	/**
	 * test update
	 */
	@Test
	void update() {
		final int id = repository.findAll(Sort.by("name")).get(0).getId();

		final ContainerScope vo = new ContainerScope();
		vo.setId(id);
		vo.setName("Name");
		vo.setDn("dc=sample,dc=com");
		vo.setType(ContainerType.GROUP);
		resource.update(vo);
		em.flush();
		em.clear();

		final ContainerScope entity = repository.findOneExpected(id);
		Assertions.assertEquals("Name", entity.getName());
		Assertions.assertEquals("dc=sample,dc=com", entity.getDn());
		Assertions.assertEquals(id, entity.getId().intValue());
	}

	/**
	 * test delete locked group
	 */
	@Test
	void deleteLocked() {
		final ContainerScope scope = repository.findAll(Sort.by("name")).get(3);
		final int id = scope.getId();
		Assertions.assertTrue(scope.isLocked());
		final long initCount = repository.count();
		em.clear();
		resource.delete(id);
		em.flush();
		em.clear();

		// Check is not deleted
		Assertions.assertEquals(initCount, repository.count());
	}

	/**
	 * test delete
	 */
	@Test
	void delete() {
		final ContainerScope scope = repository.findAll(Sort.by("name")).get(0);
		final int id = scope.getId();
		Assertions.assertFalse(scope.isLocked());
		final long initCount = repository.count();
		em.clear();
		resource.delete(id);
		em.flush();
		em.clear();
		Assertions.assertEquals(initCount - 1, repository.count());
	}
}
