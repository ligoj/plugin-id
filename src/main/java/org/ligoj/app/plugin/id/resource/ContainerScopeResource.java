/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheResult;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.dao.ContainerScopeRepository;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * Container scope resource.
 */
@Path(IdentityResource.SERVICE_URL + "/container-scope")
@Service
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class ContainerScopeResource {

	@Autowired
	private ContainerScopeRepository repository;

	@Autowired
	private PaginationJson paginationJson;

	/**
	 * Ordered columns.
	 */
	private static final Map<String, String> ORDERED_COLUMNS = new HashMap<>();
	static {
		ORDERED_COLUMNS.put("name", "name");
		ORDERED_COLUMNS.put("dn", "dn");
		ORDERED_COLUMNS.put("locked", "locked");
	}

	/**
	 * Return all {@link ContainerScope} in descendant order by DN in order to
	 * match the finest associations first.
	 * 
	 * @param type
	 *            The {@link ContainerType} to filter. Required.
	 * @return all {@link ContainerScope}.
	 */
	@CacheResult(cacheName = "container-scopes")
	public List<ContainerScope> findAllDescOrder(@CacheKey final ContainerType type) {
		return repository.findAllOrderByDnDesc(type);
	}

	/**
	 * Return all types matching to given criteria.
	 * 
	 * @param type
	 *            filtered {@link ContainerType}.
	 * @param uriInfo
	 *            Filter data including criteria.
	 * @return Found group types.
	 */
	@GET
	@Path("{type}")
	public TableItem<ContainerScope> findAll(@PathParam("type") final ContainerType type, @Context final UriInfo uriInfo) {
		final String criteria = StringUtils.trimToEmpty(DataTableAttributes.getSearch(uriInfo));
		final Page<ContainerScope> findAll = repository.findAll(type, criteria, paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS));

		// apply pagination and prevent lazy initialization issue
		return paginationJson.applyPagination(uriInfo, findAll, Function.identity());
	}

	/**
	 * Update a {@link ContainerScope}.
	 * 
	 * @param bean
	 *            new {@link ContainerScope} to update.
	 */
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@CacheRemoveAll(cacheName = "container-scopes")
	public void update(final ContainerScope bean) {
		repository.saveAndFlush(check(bean));
	}

	/**
	 * Create a new {@link ContainerScope}.
	 * 
	 * @param bean
	 *            new {@link ContainerScope} to persist.
	 * @return new identifier.
	 */
	@POST
	@CacheRemoveAll(cacheName = "container-scopes")
	@Consumes(MediaType.APPLICATION_JSON)
	public int create(final ContainerScope bean) {
		return repository.saveAndFlush(check(bean)).getId();
	}

	/**
	 * Validate and clean the type.
	 * 
	 * @param entity
	 *            The entity to check.
	 * @return The given parameter.
	 */
	protected ContainerScope check(final ContainerScope entity) {
		entity.setDn(StringUtils.trimToNull(entity.getDn()));
		entity.setName(StringUtils.trimToNull(entity.getName()));
		return entity;
	}

	/**
	 * Retrieve a type by its identifier.
	 * 
	 * @param id
	 *            Type identifier.
	 * @return Corresponding {@link ContainerScope}.
	 */
	@GET
	@Path("{id:\\d+}")
	public ContainerScope findById(@PathParam("id") final int id) {
		return repository.findOneExpected(id);
	}

	/**
	 * Retrieve a type by its name.
	 * 
	 * @param name
	 *            type name.
	 * @return corresponding {@link ContainerScope}.
	 */
	public ContainerScope findByName(final String name) {
		return repository.findByNameExpected(name);
	}

	/**
	 * Delete {@link ContainerScope} from its identifier. Only non-locked
	 * objects can be deleted.
	 * 
	 * @param id
	 *            Identifier of {@link ContainerScope} to delete.
	 */
	@DELETE
	@Path("{id}")
	@CacheRemoveAll(cacheName = "container-scopes")
	public void delete(@PathParam("id") final int id) {
		repository.delete(id);
	}

}
