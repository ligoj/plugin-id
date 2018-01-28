package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

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
public class ContainerScopeResourceTest extends AbstractJpaTest {

	@Autowired
	private ContainerScopeResource resource;

	@Autowired
	private ContainerScopeRepository repository;

	@BeforeEach
	public void setUpEntities() throws IOException {
		persistEntities("csv", new Class[] { ContainerScope.class }, "UTF-8");
	}

	@Test
	public void findAll() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();

		final TableItem<ContainerScope> result = resource.findAll(ContainerType.GROUP, uriInfo, null);
		Assertions.assertEquals(4, result.getData().size());

		final ContainerScope type = result.getData().get(1);
		checkType(type);
	}

	@Test
	public void findAll2() {
		final List<ContainerScope> result = resource.findAllDescOrder(ContainerType.GROUP);
		Assertions.assertEquals(4, result.size());
		final ContainerScope type = result.get(2);
		Assertions.assertEquals("Project", type.getName());
		Assertions.assertEquals("ou=project,dc=sample,dc=com", type.getDn());
	}

	@Test
	public void findAllCompany() {
		final List<ContainerScope> result = resource.findAllDescOrder(ContainerType.COMPANY);
		Assertions.assertEquals(2, result.size());
		final ContainerScope type = result.get(0);
		Assertions.assertEquals("France", type.getName());
		Assertions.assertEquals("ou=france,ou=people,dc=sample,dc=com", type.getDn());
		Assertions.assertEquals(ContainerType.COMPANY, type.getType());
	}

	@Test
	public void findAllGlobalSearch() {
		// create a mock URI info with pagination informations
		final UriInfo uriInfo = newFindAllParameters();

		final TableItem<ContainerScope> result = resource.findAll(ContainerType.GROUP, uriInfo, "j");
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
	public void findByIdInvalid() {
		Assertions.assertThrows(JpaObjectRetrievalFailureException.class, () -> {
			Assertions.assertNull(resource.findById(0));
		});
	}

	/**
	 * test {@link ContainerScopeResource#findById(int)}
	 */
	@Test
	public void findById() {
		final Integer id = repository.findAll(Sort.by("name")).get(3).getId();
		checkType(resource.findById(id));
	}

	@Test
	public void findByName() {
		checkType(resource.findByName("Project"));
	}

	private void checkType(final ContainerScope type) {
		Assertions.assertEquals("Project", type.getName());
		Assertions.assertTrue(type.isLocked());
		Assertions.assertEquals("ou=project,dc=sample,dc=com", type.getDn());
		Assertions.assertEquals(ContainerType.GROUP, type.getType());
	}

	/**
	 * test create
	 */
	@Test
	public void create() {
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
	public void createDuplicateDn() {
		final ContainerScope vo = new ContainerScope();
		vo.setName("Name");
		vo.setType(ContainerType.GROUP);
		vo.setDn("ou=project,dc=sample,dc=com");
		Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
			resource.create(vo);
		});
	}

	/**
	 * test create duplicate name
	 */
	@Test
	public void createDuplicateName() {
		final ContainerScope vo = new ContainerScope();
		vo.setName("Project");
		vo.setDn("dc=sample,dc=com");
		vo.setType(ContainerType.GROUP);
		Assertions.assertThrows(DataIntegrityViolationException.class, () -> {
			resource.create(vo);
		});
	}

	/**
	 * test update
	 */
	@Test
	public void update() {
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
	public void deleteLocked() {
		final ContainerScope typeLdap = repository.findAll(Sort.by("name")).get(3);
		final int id = typeLdap.getId();
		Assertions.assertTrue(typeLdap.isLocked());
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
	public void delete() {
		final ContainerScope typeLdap = repository.findAll(Sort.by("name")).get(0);
		final int id = typeLdap.getId();
		Assertions.assertFalse(typeLdap.isLocked());
		final long initCount = repository.count();
		em.clear();
		resource.delete(id);
		em.flush();
		em.clear();
		Assertions.assertEquals(initCount - 1, repository.count());
	}
}
