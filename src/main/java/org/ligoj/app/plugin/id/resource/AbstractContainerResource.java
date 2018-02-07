package org.ligoj.app.plugin.id.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.ligoj.app.api.Normalizer;
import org.ligoj.app.iam.ContainerOrg;
import org.ligoj.app.iam.IContainerRepository;
import org.ligoj.app.iam.dao.CacheContainerRepository;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.CacheContainer;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.DnUtils;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.core.NamedBean;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.resource.OnNullReturn404;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Basic container operations.
 * 
 * @param <T>
 *            The container type.
 * @param <V>
 *            The container edition bean type.
 * @param <C>
 *            The container cache type.
 */
@Slf4j
public abstract class AbstractContainerResource<T extends ContainerOrg, V extends ContainerEditionVo, C extends CacheContainer>
		extends AbstractOrgResource {

	protected static final String TYPE_ATTRIBUTE = "type";

	@Autowired
	protected ContainerScopeResource containerScopeResource;

	@Autowired
	protected PaginationJson paginationJson;

	@Autowired
	protected SecurityHelper securityHelper;

	@Autowired
	protected DelegateOrgRepository delegateRepository;

	/**
	 * The container type manager by this instance.
	 */
	protected final ContainerType type;

	/**
	 * Ordered columns.
	 */
	protected static final Map<String, String> ORDERED_COLUMNS = new HashMap<>();

	static {
		ORDERED_COLUMNS.put("name", "name");
	}

	protected AbstractContainerResource(final ContainerType type) {
		this.type = type;
	}

	/**
	 * Return the repository managing the container.
	 * 
	 * @return the repository managing the container.
	 */
	protected abstract IContainerRepository<T> getRepository();

	/**
	 * Return the repository managing the container as cache.
	 * 
	 * @return the repository managing the container as cache.
	 */
	protected abstract CacheContainerRepository<C> getCacheRepository();

	/**
	 * Return the DN from the container and the computed scope.
	 * 
	 * @param container
	 *            The container to convert.
	 * @param scope
	 *            The container scope.
	 * @return The DN from the container and the computed scope.
	 */
	protected abstract String toDn(V container, ContainerScope scope);

	/**
	 * Simple transformer, securing sensible date. DN is not forwarded.
	 * 
	 * @param rawGroup
	 *            The group to convert.
	 * @return The container including the scope and without sensible data.
	 */
	protected ContainerWithScopeVo toVo(final T rawGroup) {
		// Find the closest type
		final ContainerWithScopeVo securedUserOrg = new ContainerWithScopeVo();
		final List<ContainerScope> scopes = containerScopeResource.findAllDescOrder(type);
		final ContainerScope scope = toScope(scopes, rawGroup);
		NamedBean.copy(rawGroup, securedUserOrg);
		if (scope != null) {
			securedUserOrg.setScope(scope.getName());
		}
		return securedUserOrg;
	}

	/**
	 * Return the container matching to given name. Case is sensitive.
	 * Visibility is checked against security context. DN is not exposed.
	 * 
	 * @param name
	 *            the container name. Exact match is required, so a normalized
	 *            version.
	 * @return Container (CN) with its type.
	 */
	@GET
	@Path("{container:" + ContainerOrg.NAME_PATTERN + "}")
	@OnNullReturn404
	public ContainerWithScopeVo findByName(@PathParam("container") final String name) {
		return Optional.ofNullable(findById(name)).map(this::toVo).orElse(null);
	}

	/**
	 * Create the given container.<br>
	 * The delegation system is involved for this operation and requires
	 * administration privilege on the parent tree or group/company.
	 * 
	 * @param container
	 *            The container to create.
	 * @return The identifier of created {@link org.ligoj.app.iam.ContainerOrg}.
	 */
	@POST
	public String create(final V container) {
		return createInternal(container).getId();
	}

	/**
	 * Create the given container.<br>
	 * The delegation system is involved for this operation and requires
	 * administration privilege on the parent tree or group/company.<br>
	 * Note this is for internal use since the returned object corresponds to
	 * the internal representation.
	 * 
	 * @param container
	 *            The container to create.
	 * @return The created {@link org.ligoj.app.iam.ContainerOrg} internal
	 *         identifier.
	 */
	public T createInternal(final V container) {

		// Check the unlocked scope exists
		final ContainerScope scope = containerScopeResource.findById(container.getScope());

		// Check the type matches with this class' container type
		if (this.type != scope.getType()) {
			throw new ValidationJsonException(TYPE_ATTRIBUTE, "container-scope-match", TYPE_ATTRIBUTE, this.type, "provided",
					scope.getType());
		}

		// Build the new DN, keeping the case
		final String newDn = toDn(container, scope);

		// Check the container can be created by the current principal.
		// Used DN will be FQN to match the delegates
		if (!delegateRepository.canCreate(securityHelper.getLogin(), Normalizer.normalize(newDn), this.type.getDelegateType())) {
			// Not managed container, report this attempt and act as if this
			// container already exists
			log.warn("Attempt to create a {} '{}' out of scope", scope, container.getName());
			throw new ValidationJsonException("name", "read-only", "0", getTypeName(), "1", container.getName());
		}

		// Check the container does not exists
		if (getRepository().findById(Normalizer.normalize(container.getName())) != null) {
			throw new ValidationJsonException("name", "already-exist", "0", getTypeName(), "1", container.getName());
		}

		// Create the new container
		return create(container, scope, newDn);
	}

	protected T create(final V container, final ContainerScope type, final String newDn) {
		log.info("Creating a {}@{}-{} '{}'", this.type, type.getName(), type.getId(), container.getName());
		return getRepository().create(newDn, container.getName());
	}

	/**
	 * Delete an existing container.<br>
	 * The delegation system is involved for this operation and requires
	 * administration privilege on this container.
	 * 
	 * @param id
	 *            The container's identifier.
	 */
	@DELETE
	@Path("{id}")
	public void delete(@PathParam("id") final String id) {
		// Check the container exists
		final T container = findByIdExpected(id);

		// Check the container can be deleted by current user
		checkForDeletion(container);

		// Perform the deletion when checked
		getRepository().delete(container);
	}

	/**
	 * Return containers the current user can manage with write access.
	 * 
	 * @param uriInfo
	 *            filter data.
	 * @return containers the current user can manage.
	 */
	@GET
	@Path("filter/write")
	public TableItem<String> getContainersForWrite(@Context final UriInfo uriInfo) {
		return paginationJson.applyPagination(uriInfo, getCacheRepository().findAllWrite(securityHelper.getLogin(),
				DataTableAttributes.getSearch(uriInfo), paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS)), CacheContainer::getName);
	}

	/**
	 * Return containers the current user can manage with administration access.
	 * 
	 * @param uriInfo
	 *            filter data.
	 * @return containers the current user can manage.
	 */
	@GET
	@Path("filter/admin")
	public TableItem<String> getContainersForAdmin(@Context final UriInfo uriInfo) {
		return paginationJson.applyPagination(uriInfo, getCacheRepository().findAllAdmin(securityHelper.getLogin(),
				DataTableAttributes.getSearch(uriInfo), paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS)), CacheContainer::getName);
	}

	/**
	 * Return containers the current user can see. A user always sees his
	 * company, as if he had a company delegation to see it.
	 * 
	 * @param uriInfo
	 *            filter data.
	 * @return containers the current user can see.
	 */
	@GET
	@Path("filter/read")
	public TableItem<String> getContainers(@Context final UriInfo uriInfo) {
		return paginationJson.applyPagination(uriInfo, getCacheRepository().findAll(securityHelper.getLogin(),
				DataTableAttributes.getSearch(uriInfo), paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS)), CacheContainer::getName);
	}

	/**
	 * Find a container from its identifier. If the container is not found or
	 * cannot be seen by the current user, the error code
	 * {@link org.ligoj.bootstrap.core.resource.BusinessException#KEY_UNKNOW_ID}
	 * will be returned.
	 * 
	 * @param id
	 *            The container's identifier. Will be normalized.
	 * @return The container from its identifier.
	 */
	public T findByIdExpected(final String id) {
		return getRepository().findByIdExpected(securityHelper.getLogin(), id);
	}

	/**
	 * Find a container from its identifier.
	 * 
	 * @param id
	 *            The container's identifier. Will be normalized.
	 * @return The container from its identifier. <code>null</code> if the
	 *         container is not found or cannot be seen by the current user
	 */
	public T findById(final String id) {
		return getRepository().findById(securityHelper.getLogin(), id);
	}

	/**
	 * Check the container can be deleted by the current user.
	 * 
	 * @param container
	 *            The container to delete.
	 */
	protected void checkForDeletion(final ContainerOrg container) {

		// Check the container can be deleted by the current user. Used DN will
		// be FQN to match the delegates
		if (!delegateRepository.canCreate(securityHelper.getLogin(), Normalizer.normalize(container.getDn()), this.type.getDelegateType())) {
			// Not managed container, report this attempt and act as if this
			// company did not exist
			log.warn("Attempt to delete a {} '{}' out of scope", type, container.getName());
			throw new ValidationJsonException(getTypeName(), BusinessException.KEY_UNKNOW_ID, "0", getTypeName(), "1", container.getId());
		}

		// Check this container is not locked
		if (container.isLocked()) {
			throw new ValidationJsonException("company", "locked", "0", container.getName());
		}
	}

	/**
	 * Return the closest {@link ContainerScope} name associated to the given
	 * container. Order of scopes is important since the first matching item
	 * from this list is returned.
	 * 
	 * @param scopes
	 *            The available scopes.
	 * @param container
	 *            The containers to check.
	 * @return The closest {@link ContainerScope} or <code>null</code> if not
	 *         found.
	 */
	public ContainerScope toScope(final List<ContainerScope> scopes, final ContainerOrg container) {
		return scopes.stream().filter(s -> DnUtils.equalsOrParentOf(s.getDn(), container.getDn())).findFirst().orElse(null);
	}

	/**
	 * Order {@link ContainerScope} by container type.
	 */
	@AllArgsConstructor
	public class TypeComparator implements Comparator<T> {

		/**
		 * Container types
		 */
		private List<ContainerScope> types;

		@Override
		public int compare(final T container1, final T container2) {
			final int result;

			// First compare the type
			final ContainerScope type1 = toScope(types, container1);
			final ContainerScope type2 = toScope(types, container2);
			if (Objects.equals(type1, type2)) {
				result = 0;
			} else if (type1 == null) {
				result = 1;
			} else if (type2 == null) {
				result = -1;
			} else {
				result = type1.getName().compareToIgnoreCase(type2.getName());
			}

			// Then the compare the container name
			if (result == 0) {
				return container1.getName().compareToIgnoreCase(container2.getName());
			}
			return result;
		}
	}

	/**
	 * Build a new secured container managing the effective visibility and
	 * rights.
	 * 
	 * @param rawContainer
	 *            the raw container contained sensitive data.
	 * @param canWrite
	 *            The containers the principal user can write.
	 * @param canAdmin
	 *            The containers the principal user can administer.
	 * @param types
	 *            The defined type with locking information.
	 * @return A secured container with right and lock information the current
	 *         user has.
	 */
	protected ContainerCountVo newContainerCountVo(final ContainerOrg rawContainer, final Set<T> canWrite, final Set<T> canAdmin,
			final List<ContainerScope> types) {
		final ContainerCountVo securedUserOrg = new ContainerCountVo();
		NamedBean.copy(rawContainer, securedUserOrg);
		securedUserOrg.setCanWrite(canWrite.contains(rawContainer));
		securedUserOrg.setCanAdmin(canAdmin.contains(rawContainer));
		securedUserOrg.setContainerType(type);

		// Find the closest type
		final ContainerScope scope = toScope(types, rawContainer);
		if (scope != null) {
			securedUserOrg.setScope(scope.getName());
			securedUserOrg.setLocked(scope.isLocked());
		}
		securedUserOrg.setLocked(securedUserOrg.isLocked() || rawContainer.isLocked());
		return securedUserOrg;
	}

	/**
	 * Return containers the given user can manage with write access.
	 * 
	 * @return ordered containers the given user can manage with write access.
	 */
	public Set<T> getContainersForWrite() {
		return toInternal(getCacheRepository().findAllWrite(securityHelper.getLogin()));
	}

	/**
	 * Return containers the given user can manage with administration access.
	 * 
	 * @return ordered companies the given user can manage with administration
	 *         access.
	 */
	protected Set<T> getContainersForAdmin() {
		return toInternal(getCacheRepository().findAllAdmin(securityHelper.getLogin()));
	}

	/**
	 * Return containers the current user can see.
	 * 
	 * @return ordered containers the current user can see.
	 */
	public Set<T> getContainers() {
		return toInternal(getCacheRepository().findAll(securityHelper.getLogin()));
	}

	/**
	 * Return containers the current user can see.
	 * 
	 * @param criteria
	 *            Optional criteria, can be <code>null</code>.
	 * @param pageRequest
	 *            Optional {@link Pageable}, can be <code>null</code>.
	 * @return ordered containers the current user can see.
	 */
	public Page<T> getContainers(final String criteria, final Pageable pageRequest) {
		return toInternal(getCacheRepository().findAll(securityHelper.getLogin(), criteria, pageRequest));
	}

	/**
	 * Return the internal representation of the container set. Not existing
	 * cache items are removed.
	 * 
	 * @param cacheItems
	 *            The database base cache containers to convert.
	 * @return The internal representation of container set. Ordered is kept.
	 */
	protected Set<T> toInternal(final Collection<C> cacheItems) {
		return cacheItems.stream().map(CacheContainer::getId).map(getRepository().findAll()::get).filter(Objects::nonNull)
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	/**
	 * Return the internal representation of the container set as a
	 * {@link Page}.
	 * 
	 * @param cacheItems
	 *            The database base page cache containers to convert.
	 * @return The internal representation of
	 *         {@link org.ligoj.app.iam.model.CacheCompany} set. Ordered by the
	 *         name.
	 */
	protected Page<T> toInternal(final Page<C> cacheItems) {
		return new PageImpl<>(new ArrayList<>(toInternal(cacheItems.getContent())), cacheItems.getPageable(),
				cacheItems.getTotalElements());
	}

	protected String getTypeName() {
		return getRepository().getTypeName();
	}
}
