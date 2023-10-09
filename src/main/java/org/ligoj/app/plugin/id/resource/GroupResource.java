/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.Normalizer;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.dao.CacheGroupRepository;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.DnUtils;
import org.ligoj.app.plugin.id.dao.IdCacheDao;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Group resource.
 */
@Path(IdentityResource.SERVICE_URL + "/group")
@Service
@Produces(MediaType.APPLICATION_JSON)
@Transactional
public class GroupResource extends AbstractContainerResource<GroupOrg, GroupEditionVo, CacheGroup> {

	/**
	 * Attribute name used as filter and path.
	 */
	public static final String GROUP_ATTRIBUTE = IdCacheDao.GROUP_ATTRIBUTE;

	@Autowired
	private CompanyResource organizationResource;

	@Autowired
	private CacheGroupRepository cacheGroupRepository;

	/**
	 * Default constructor specifying the type as {@link ContainerType#GROUP}
	 */
	public GroupResource() {
		super(ContainerType.GROUP);
	}

	@Override
	public IGroupRepository getRepository() {
		return getGroup();
	}

	@Override
	public CacheGroupRepository getCacheRepository() {
		return cacheGroupRepository;
	}

	/**
	 * Return groups matching to given criteria. The visible groups, trees and companies are checked. The returned
	 * groups of each user depends on the groups the user can see/write in CN form.
	 *
	 * @param uriInfo filter data.
	 * @return found groups.
	 */
	@GET
	public TableItem<ContainerCountVo> findAll(@Context final UriInfo uriInfo) {
		final var types = containerScopeResource.findAllDescOrder(ContainerType.GROUP);
		final var companies = getCompany().findAll();
		final var visibleCompanies = organizationResource.getContainers();
		final var writeGroups = getContainersIdForWrite();
		final var adminGroups = getContainersIdForAdmin();
		final var users = getUser().findAll();
		final var groups = getGroup().findAll();

		// Search the groups
		final var page = getContainers(DataTableAttributes.getSearch(uriInfo),
				paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS));

		// Apply pagination and secure the users data
		return paginationJson.applyPagination(uriInfo, page, rawGroup -> {
			final var securedGroup = new ContainerCountVo();
			fillContainerCountVo(rawGroup, writeGroups, adminGroups, types, securedGroup, groups);
			securedGroup.setCount(rawGroup.getMembers().size());
			// Computed the visible members
			securedGroup.setCountVisible((int) rawGroup.getMembers().stream().map(users::get).map(UserOrg::getCompany)
					.map(companies::get).map(CompanyOrg::getCompanyTree)
					.filter(c -> CollectionUtils.containsAny(visibleCompanies, c)).count());
			return securedGroup;
		});
	}

	/**
	 * Indicates a group exists or not.
	 *
	 * @param group the group name. Exact match is required, so a normalized version.
	 * @return <code>true</code> if the group exists.
	 */
	@GET
	@Path("{group}/exists")
	public boolean exists(@PathParam(GROUP_ATTRIBUTE) final String group) {
		return findById(group) != null;
	}

	@Override
	protected String toDn(final GroupEditionVo container, final ContainerScope scope) {
		var parentDn = scope.getDn();
		container.setParent(StringUtils.trimToNull(Normalizer.normalize(container.getParent())));
		if (container.getParent() != null) {
			// Check the parent is also inside the type, a new DN will be built
			final var parent = findByIdExpected(container.getParent());
			if (!DnUtils.equalsOrParentOf(scope.getDn(), parent.getDn())) {
				throw new ValidationJsonException("parent", "container-parent-type-match", TYPE_ATTRIBUTE, this.type,
						"provided", scope.getType());
			}
			parentDn = parent.getDn();
		}

		return "cn=" + container.getName() + "," + parentDn;
	}

	/**
	 * Convert the given user UIDs to the corresponding DN. The users must exist.
	 *
	 * @param uids The UIDs to convert.
	 * @return The corresponding DN.
	 */
	private List<String> toDn(final List<String> uids) {
		return CollectionUtils.emptyIfNull(uids).stream().map(getUser()::findByIdExpected).map(UserOrg::getDn).toList();
	}

	/**
	 * Empty this group by removing all members if supported by the repository.
	 *
	 * @param id The group to empty.
	 */
	@POST
	@Path("empty/{id}")
	public void empty(@PathParam("id") final String id) {
		// Check the group exists
		final var container = findByIdExpected(id);

		// Check the group can be updated by the current user
		if (!getContainersForWrite().contains(container)) {
			throw new ValidationJsonException(getTypeName(), BusinessException.KEY_UNKNOWN_ID, "0", getTypeName(), "1",
					id);
		}

		// Perform the update
		getRepository().empty(container, getUser().findAll());
	}

	@Override
	protected GroupOrg create(final GroupEditionVo container, final ContainerScope type, final String newDn) {
		// Check the related objects
		final var assistants = toDn(container.getAssistants());
		final var owners = toDn(container.getOwners());

		// Create the group
		final var group = super.create(container, type, newDn);

		// Nesting management
		if (container.getParent() != null) {
			// This group will be added as "uniqueMember" of its parent
			getRepository().addGroup(group, Normalizer.normalize(container.getParent()));
		}

		// Assistant/Owner/Department management
		getRepository().addAttributes(newDn, "seeAlso", assistants);
		getRepository().addAttributes(newDn, "owner", owners);
		getRepository().addAttributes(newDn, "businessCategory",
				CollectionUtils.emptyIfNull(container.getDepartments()));

		return group;
	}
}
