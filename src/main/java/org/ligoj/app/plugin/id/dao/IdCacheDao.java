/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;
import javax.transaction.Transactional;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.ContainerOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheContainer;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.iam.model.ReceiverType;
import org.ligoj.app.model.CacheProjectGroup;
import org.ligoj.app.model.Project;
import org.ligoj.bootstrap.core.DescribedBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;

/**
 * Cache synchronization from SQL cache to database.
 */
@Transactional
@Repository
@Slf4j
public class IdCacheDao {

	@PersistenceContext(type = PersistenceContextType.TRANSACTION, unitName = "pu")
	private EntityManager em;

	@Autowired
	private CacheProjectGroupRepository cacheProjectGroupRepository;

	@Autowired
	private DelegateOrgRepository delegateOrgRepository;

	/**
	 * Add a group to a group.
	 *
	 * @param subGroup
	 *            the group to add the parent group.
	 * @param group
	 *            the group to update.
	 */
	public void addGroupToGroup(final GroupOrg subGroup, final GroupOrg group) {
		addGroupToGroupInternal(em.find(CacheGroup.class, subGroup.getId()), group);
	}

	/**
	 * Associate a group to another group.
	 */
	private void addGroupToGroupInternal(final CacheGroup entity, final GroupOrg group) {
		final var membership = new CacheMembership();
		final var cacheGroup = new CacheGroup();
		cacheGroup.setId(group.getId());
		membership.setSubGroup(entity);
		membership.setGroup(cacheGroup);
		em.persist(membership);
	}

	/**
	 * Add a user to a group.
	 *
	 * @param user
	 *            the user to add to the group.
	 * @param group
	 *            the group to update.
	 */
	public void addUserToGroup(final UserOrg user, final GroupOrg group) {
		addUserToGroupInternal(em.find(CacheUser.class, user.getId()), em.find(CacheGroup.class, group.getId()));
	}

	/**
	 * Associate a user to a group.
	 */
	private void addUserToGroupInternal(final CacheUser entity, final CacheGroup group) {
		final var membership = new CacheMembership();
		membership.setUser(entity);
		membership.setGroup(group);
		em.persist(membership);
	}

	/**
	 * Remove all data from database.
	 */
	public void clear() {
		em.createQuery("DELETE FROM CacheProjectGroup").executeUpdate();
		em.createQuery("DELETE FROM CacheMembership").executeUpdate();
		em.createQuery("DELETE FROM CacheUser").executeUpdate();
		em.createQuery("DELETE FROM CacheGroup").executeUpdate();
		em.createQuery("DELETE FROM CacheCompany").executeUpdate();
		em.flush();
		em.clear();
	}

	/**
	 * Persist a new company and flush.
	 *
	 * @param company
	 *            the company to persist.
	 * @return The persisted {@link CacheCompany}
	 */
	public CacheCompany create(final CompanyOrg company) {
		return createInternal(company);
	}

	/**
	 * Persist a new group and flush.
	 *
	 * @param group
	 *            the group to persist.
	 * @return The persisted {@link CacheGroup}
	 */
	public CacheGroup create(final GroupOrg group) {
		return createInternal(group);
	}

	/**
	 * Persist a new user and flush
	 *
	 * @param user
	 *            the user to persist.
	 */
	public void create(final UserOrg user) {
		final var entity = toCacheUser(user);

		// Set the company if defined
		entity.setCompany(Optional.ofNullable(user.getCompany()).map(c -> {
			final var company = new CacheCompany();
			company.setId(user.getCompany());
			return company;
		}).orElse(null));
		em.persist(entity);
		em.flush();
		em.clear();
	}

	/**
	 * Persist a new company
	 */
	private CacheCompany createInternal(final CompanyOrg company) {
		final var cacheCompany = toCacheCompany(company);
		em.persist(cacheCompany);
		return cacheCompany;
	}

	/**
	 * Persist a new group and return it.
	 */
	private CacheGroup createInternal(final GroupOrg group) {
		final var entity = toCacheGroup(group);
		em.persist(entity);
		return entity;
	}

	/**
	 * Persist a new user
	 */
	private CacheUser createInternal(final UserOrg user, final Map<String, CacheCompany> companies) {
		final var entity = toCacheUserInternal(user);

		// Set the company if defined
		entity.setCompany(Optional.ofNullable(user.getCompany()).map(companies::get).orElse(null));
		em.persist(entity);
		return entity;
	}

	/**
	 * Delete a company. Warning, it is assumed there is no more user associated to the deleted company.
	 *
	 * @param company
	 *            the company to delete.
	 */
	public void delete(final CompanyOrg company) {
		em.createQuery("DELETE FROM CacheCompany WHERE id=:id").setParameter("id", company.getId()).executeUpdate();
		em.flush();
		em.clear();
	}

	/**
	 * Delete a group.
	 *
	 * @param group
	 *            the group to delete.
	 */
	public void delete(final GroupOrg group) {
		em.createQuery("DELETE FROM CacheProjectGroup WHERE group.id=:id").setParameter("id", group.getId())
				.executeUpdate();
		em.createQuery("DELETE FROM CacheMembership WHERE group.id=:id OR subGroup.id=:id")
				.setParameter("id", group.getId()).executeUpdate();
		em.createQuery("DELETE FROM CacheGroup WHERE id=:id").setParameter("id", group.getId()).executeUpdate();
		em.flush();
		em.clear();
	}

	/**
	 * Delete a user.
	 *
	 * @param user
	 *            the user to delete.
	 */
	public void delete(final UserOrg user) {
		em.createQuery("DELETE FROM CacheMembership WHERE user.id=:id").setParameter("id", user.getId())
				.executeUpdate();
		em.createQuery("DELETE FROM CacheUser WHERE id=:id").setParameter("id", user.getId()).executeUpdate();
		em.flush();
		em.clear();
	}

	/**
	 * Remove all user membership to this group. Subgroups are not removed.
	 *
	 * @param group
	 *            the group to empty.
	 */
	public void empty(final GroupOrg group) {
		em.createQuery("DELETE FROM CacheMembership WHERE group.id=:id").setParameter("id", group.getId())
				.executeUpdate();
		em.flush();
		em.clear();
	}

	/**
	 * Copy data from the memory object to the cache entity.
	 */
	private <T extends CacheContainer> T fillCacheContainer(final ContainerOrg container, final T entity) {
		DescribedBean.copy(container, entity);
		return entity;
	}

	/**
	 * Persist companies and return saved entities
	 */
	private Map<String, CacheCompany> persistCompanies(final Map<String, CompanyOrg> companies) {
		return companies.values().stream().map(this::create)
				.collect(Collectors.toMap(CacheCompany::getId, Function.identity()));
	}

	/**
	 * Persist groups and return saved entities
	 */
	private Map<String, CacheGroup> persistGroups(final Map<String, GroupOrg> groups) {
		return groups.values().stream().map(this::create)
				.collect(Collectors.toMap(CacheGroup::getId, Function.identity()));
	}

	/**
	 * Persist association between users and groups.
	 *
	 * @param users
	 *            The existing users.
	 * @param cacheGroups
	 *            The groups already persisted in database.
	 * @return the amount of persisted relations.
	 */
	private int persistMemberships(final Map<String, UserOrg> users, final Map<String, CacheGroup> cacheGroups,
			final Map<String, CacheCompany> cacheCompanies) {
        var memberships = 0;
		for (final var user : users.values()) {

			// Persist users
			final var entity = createInternal(user, cacheCompanies);
			for (final var group : user.getGroups()) {
				addUserToGroupInternal(entity, cacheGroups.get(group));
				memberships++;
			}
		}
		return memberships;
	}

	/**
	 * Persist association between project and groups.
	 *
	 * @param groups
	 *            The groups already persisted in database.
	 * @return the amount of persisted relations.
	 */
	private int persistProjectGroups(final Map<String, CacheGroup> groups) {
		final var allProjectGroup = cacheProjectGroupRepository.findAllProjectGroup();
		for (final var projectGroup : allProjectGroup) {
			final var project = new Project();
			project.setId((int) projectGroup[0]);
			final var entity = new CacheProjectGroup();
			entity.setProject(project);
			entity.setGroup(groups.get(projectGroup[1]));
			em.persist(entity);
		}
		return allProjectGroup.size();
	}

	/**
	 * Remove a group from a group.
	 *
	 * @param subGroup
	 *            the user to remove from the group
	 * @param group
	 *            the group to update.
	 */
	public void removeGroupFromGroup(final GroupOrg subGroup, final GroupOrg group) {
		em.createQuery("DELETE FROM CacheMembership WHERE subGroup.id=:subGroup AND group.id=:group")
				.setParameter("group", group.getId()).setParameter("subGroup", subGroup.getId()).executeUpdate();
	}

	/**
	 * Remove a user from a group.
	 *
	 * @param user
	 *            the user to remove from the group
	 * @param group
	 *            the group to update.
	 */
	public void removeUserFromGroup(final UserOrg user, final GroupOrg group) {
		em.createQuery("DELETE FROM CacheMembership WHERE user.id=:user AND group.id=:group")
				.setParameter("group", group.getId()).setParameter("user", user.getId()).executeUpdate();
	}

	/**
	 * Reset the database cache with the provided groups/companies and users.
	 *
	 * @param users
	 *            All users.
	 * @param groups
	 *            All groups.
	 * @param companies
	 *            All companies.
	 */
	public void reset(final Map<String, CompanyOrg> companies, final Map<String, GroupOrg> groups,
			final Map<String, UserOrg> users) {
		final var start = System.currentTimeMillis();

		// Remove all CACHE_* entries
		log.info("Clearing database ...");
		clear();

		// Insert data into database
		log.info("Inserting data ...");
		final var cacheCompanies = persistCompanies(companies);
		em.flush();
		final var cacheGroups = persistGroups(groups);
		em.flush();
		final var memberships = persistMemberships(users, cacheGroups, cacheCompanies);
		em.flush();
		final var subscribedProjects = persistProjectGroups(cacheGroups);
		em.flush();
		final var updatedDelegate = updateDelegateDn(cacheGroups, cacheCompanies);
		em.flush();
		em.clear();
		log.info(
				"Done in {} : {} groups, {} companies, {} users, {} memberships, {} project groups, {} updated delegates",
				DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start), cacheGroups.size(),
				cacheCompanies.size(), users.size(), memberships, subscribedProjects, updatedDelegate);
	}

	/**
	 * Transform company to JPA.
	 */
	private CacheCompany toCacheCompany(final CompanyOrg company) {
		return fillCacheContainer(company, new CacheCompany());
	}

	/**
	 * Transform group to JPA.
	 */
	private CacheGroup toCacheGroup(final GroupOrg group) {
		return fillCacheContainer(group, new CacheGroup());
	}

	/**
	 * Transform user to JPA.
	 */
	private CacheUser toCacheUser(final UserOrg user) {
		final var entity = toCacheUserInternal(user);

		// Set the company if defined
		entity.setCompany(Optional.ofNullable(user.getCompany()).map(c -> {
			final var company = new CacheCompany();
			company.setId(user.getCompany());
			return company;
		}).orElse(null));
		return entity;
	}

	/**
	 * Transform user to JPA.
	 */
	private CacheUser toCacheUserInternal(final UserOrg user) {
		final var entity = new CacheUser();
		entity.setId(user.getId());
		entity.setFirstName(user.getFirstName());
		entity.setLastName(user.getLastName());
		if (CollectionUtils.isNotEmpty(user.getMails())) {
			entity.setMails(user.getMails().get(0));
		}
		return entity;
	}

	/**
	 * Update given user.
	 *
	 * @param user
	 *            user to update.
	 */
	public void update(final UserOrg user) {
		final var entity = toCacheUser(user);
		em.merge(entity);
		em.flush();
		em.clear();
	}

	private long updateDelegateDn(final Map<String, ? extends CacheContainer> containers, final Object type,
			final String typePath, final Function<DelegateOrg, String> id, Function<DelegateOrg, String> getDn,
			BiConsumer<DelegateOrg, String> setDn) {
		final var updated = new AtomicInteger();
		// Get all delegates of he related receiver type
		delegateOrgRepository.findAllBy(typePath, type).stream().peek(d -> {
			// Consider only the existing ones
			final var dn = Optional.ofNullable(containers.get(id.apply(d))).map(CacheContainer::getDescription)
					.orElse(null);

			// Consider only the dirty one
			final var delegateDn = getDn.apply(d);
			if (!delegateDn.equalsIgnoreCase(dn)) {
				// The delegate DN needed this update
				setDn.accept(d, dn);
				updated.incrementAndGet();
			}
		}).filter(d -> getDn.apply(d) == null).forEach(delegateOrgRepository::delete);
		return updated.get();
	}

	/**
	 * Update the receiver DN of delegates having an old DN. Delete all delegate having an invalid relation.
	 *
	 * @param containers
	 *            The existing containers.
	 * @param receiverType
	 *            The receiver type to update. And also the same type as the given containers.
	 * @param resourceType
	 *            The delegate resource type to update. And also the same type as the given containers.
	 * @return The amount of updated DN references.
	 */
	private long updateDelegateDn(final Map<String, ? extends CacheContainer> containers,
			final ReceiverType receiverType, final DelegateType resourceType) {
        var count = updateDelegateDn(containers, receiverType, "receiverType", DelegateOrg::getReceiver,
				DelegateOrg::getReceiverDn, DelegateOrg::setReceiverDn);
		count += updateDelegateDn(containers, resourceType, "type", DelegateOrg::getName, DelegateOrg::getDn,
				DelegateOrg::setDn);
		return count;
	}

	/**
	 * Update the receiver DN of delegates where the receiver is a container.
	 */
	private long updateDelegateDn(final Map<String, CacheGroup> groups, final Map<String, CacheCompany> companies) {
		return updateDelegateDn(groups, ReceiverType.GROUP, DelegateType.GROUP)
				+ updateDelegateDn(companies, ReceiverType.COMPANY, DelegateType.COMPANY);
	}

}
