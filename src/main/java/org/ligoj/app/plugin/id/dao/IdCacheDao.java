/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.PersistenceContextType;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.ContainerOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.*;
import org.ligoj.app.model.CacheProjectGroup;
import org.ligoj.app.model.Project;
import org.ligoj.bootstrap.core.DescribedBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Persistable;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

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

	@Getter
	private long cacheRefreshTime = 0;

	/**
	 * Add a group to a group.
	 *
	 * @param subGroup the group to add the parent group.
	 * @param group    the group to update.
	 */
	public void addGroupToGroup(final GroupOrg subGroup, final GroupOrg group) {
		addGroupToGroupInternal(em.find(CacheGroup.class, subGroup.getId()), group);
	}

	/**
	 * Associate a group to another group.
	 */
	private void addGroupToGroupInternal(final CacheGroup entity, final GroupOrg group) {
		updateGroupToGroupInternal(entity, em.find(CacheGroup.class, group.getId()), Collections.emptySet());
	}

	/**
	 * Add a user to a group.
	 *
	 * @param user  the user to add to the group.
	 * @param group the group to update.
	 */
	public void addUserToGroup(final UserOrg user, final GroupOrg group) {
		updateUserToGroupInternal(em.find(CacheUser.class, user.getId()), em.find(CacheGroup.class, group.getId()), Collections.emptySet());
	}

	/**
	 * Associate a user to a group.
	 */
	private void updateUserToGroupInternal(final CacheUser entity, final CacheGroup group, final Set<String> cacheGroups) {
		if (!cacheGroups.contains(group.getId())) {
			// New membership
			final var membership = new CacheMembership();
			membership.setUser(entity);
			membership.setGroup(group);
			em.persist(membership);
		}
	}

	/**
	 * Associate a subgroup to a group.
	 */
	private void updateGroupToGroupInternal(final CacheGroup subGroup, final CacheGroup group, final Set<String> cacheGroups) {
		if (!cacheGroups.contains(group.getId())) {
			// New membership
			final var membership = new CacheMembership();
			membership.setSubGroup(subGroup);
			membership.setGroup(group);
			em.persist(membership);
		}
	}

	/**
	 * Persist a new company and flush.
	 *
	 * @param company  the company to persist.
	 * @param entities the existing entities by ID.
	 * @return The persisted {@link CacheCompany}
	 */
	public CacheCompany create(final CompanyOrg company, final Map<String, CacheCompany> entities) {
		return createInternal(company, entities);
	}

	/**
	 * Persist a new company and flush.
	 *
	 * @param company the company to persist.
	 * @return The persisted {@link CacheCompany}
	 */
	public CacheCompany create(final CompanyOrg company) {
		return createInternal(company, Collections.emptyMap());
	}

	/**
	 * Persist a new group and flush.
	 *
	 * @param group    the group to persist.
	 * @param entities the existing entities by ID.
	 * @return The persisted {@link CacheGroup}
	 */
	public CacheGroup create(final GroupOrg group, final Map<String, CacheGroup> entities) {
		return createInternal(group, entities);
	}

	/**
	 * Persist a new user and flush
	 *
	 * @param user the user to persist.
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
	 * Persist a new company. Depending on the cached entities, a 'merge' or a 'persist' is executed.
	 */
	private CacheCompany createInternal(final CompanyOrg company, final Map<String, CacheCompany> entities) {
		final CacheCompany entity;
		if (entities.containsKey(company.getId())) {
			// Update as needed
			entity = toCacheCompany(company, entities.get(company.getId()));
			em.merge(entity);
		} else {
			entity = toCacheCompany(company, new CacheCompany());
			em.persist(entity);
		}
		return entity;
	}

	/**
	 * Persist/update a group and return it.
	 */
	private CacheGroup createInternal(final GroupOrg group, final Map<String, CacheGroup> entities) {
		final CacheGroup entity;
		if (entities.containsKey(group.getId())) {
			// Update as needed
			entity = toCacheGroup(group, entities.get(group.getId()));
			em.merge(entity);
		} else {
			entity = toCacheGroup(group, new CacheGroup());
			em.persist(entity);
		}
		return entity;
	}

	/**
	 * Persist/update new user and return it.
	 */
	private CacheUser createInternal(final UserOrg user, final Map<String, CacheUser> entities, final Map<String, CacheCompany> companies) {
		final CacheUser entity;
		if (entities.containsKey(user.getId())) {
			// Update as needed
			entity = toCacheUserInternal(user, entities.get(user.getId()), companies);
			em.merge(entity);
		} else {
			entity = toCacheUserInternal(user, new CacheUser(), companies);
			em.persist(entity);
		}

		return entity;
	}

	/**
	 * Delete a company. Warning, it is assumed there is no more user associated to the deleted company.
	 *
	 * @param company the company to delete.
	 */
	public void delete(final CompanyOrg company) {
		em.createQuery("DELETE FROM CacheCompany WHERE id=:id").setParameter("id", company.getId()).executeUpdate();
		em.flush();
		em.clear();
	}

	/**
	 * Delete a group.
	 *
	 * @param group the group to delete.
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
	 * @param user the user to delete.
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
	 * @param group the group to empty.
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
	 * Persist association between users and groups.
	 *
	 * @param users       The new users reference.
	 * @param groups      The new groups reference.
	 * @param cacheGroups The groups already persisted in database.
	 * @return the amount of persisted relations.
	 */
	private int persistUsersAndMemberships(final Map<String, UserOrg> users, final Map<String, GroupOrg> groups, final Map<String, CacheGroup> cacheGroups,
			final Map<String, CacheCompany> cacheCompanies) {
		final var cacheUsers = em.createQuery("FROM CacheUser", CacheUser.class)
				.getResultList().stream().collect(Collectors.toMap(CacheUser::getId, Function.identity()));
		final var userMemberships = em.createQuery("FROM CacheMembership WHERE user != null", CacheMembership.class)
				.getResultList().stream().collect(
						Collectors.groupingBy(c -> c.getUser().getId(),
								Collectors.mapping(c -> c.getGroup().getId(),
										Collectors.toSet())));
		final var groupMemberships = em.createQuery("FROM CacheMembership WHERE subGroup != null", CacheMembership.class)
				.getResultList().stream().collect(
						Collectors.groupingBy(c -> c.getGroup().getId(),
								Collectors.mapping(c -> c.getSubGroup().getId(),
										Collectors.toSet())));
		var memberships = 0;

		// Persist users and memberships
		for (final var user : users.values()) {
			// Create/update user
			final var entity = createInternal(user, cacheUsers, cacheCompanies);

			// Create/update membership
			final var cacheUserGroups = userMemberships.getOrDefault(user.getId(), Collections.emptySet());
			for (final var group : user.getGroups()) {
				updateUserToGroupInternal(entity, cacheGroups.get(group), cacheUserGroups);
			}
			memberships += user.getGroups().size();

			// Remove old memberships
			cacheUserGroups.removeAll(user.getGroups());
			cacheUserGroups.forEach(g -> {
				log.info("Deleting removed cache entry {}#{}-{} (user/group)", CacheMembership.class.getSimpleName(), user.getId(), g);
				em.createQuery("DELETE FROM CacheMembership WHERE user.id=:user and group.id=:group")
						.setParameter("user", user.getId())
						.setParameter("group", g)
						.executeUpdate();
			});
		}

		// Persist subgroups and memberships
		for (final var group : groups.values()) {
			final var cachedGroup = cacheGroups.get(group.getId());
			final var cacheSubGroups = groupMemberships.getOrDefault(group.getId(), Collections.emptySet());
			for (final var subGroup : group.getSubGroups()) {
				updateGroupToGroupInternal(cacheGroups.get(subGroup), cachedGroup, cacheSubGroups);
			}
			memberships += group.getSubGroups().size();

			// Remove old memberships
			cacheSubGroups.removeAll(group.getSubGroups());
			cacheSubGroups.forEach(g -> {
				log.info("Deleting removed cache entry {}#{}-{} (sub/group)", CacheMembership.class.getSimpleName(), g, group.getId());
				em.createQuery("DELETE FROM CacheMembership WHERE subGroup.id=:subGroup and group.id=:group")
						.setParameter("subGroup", g)
						.setParameter("group", group.getId())
						.executeUpdate();
			});
		}

		// Remove old users and related membership
		deleteOldCacheEntities(CacheUser.class, cacheUsers, users, u ->
				em.createQuery("DELETE FROM CacheMembership WHERE user.id=:user")
						.setParameter("user", u)
						.executeUpdate());
		return memberships;
	}

	/**
	 * Persist association between project and groups.
	 *
	 * @param groups The groups already persisted in database.
	 * @return the amount of persisted relations.
	 */
	private int persistProjectGroups(final Map<String, CacheGroup> groups) {
		final var entities = em.createQuery("FROM CacheProjectGroup WHERE group != null", CacheProjectGroup.class)
				.getResultList().stream().collect(
						Collectors.groupingBy(c -> c.getProject().getId(),
								Collectors.mapping(c -> c.getGroup().getId(),
										Collectors.toSet())));

		final var allProjectGroup = cacheProjectGroupRepository.findAllProjectGroup();
		for (final var projectGroup : allProjectGroup) {
			final var projectId = (int) projectGroup[0];
			final var groupId = (String) projectGroup[1];
			final var projectGroupIds = entities.get(projectId);
			if (projectGroupIds == null || !projectGroupIds.contains(groupId)) {
				// New association
				final var project = new Project();
				project.setId(projectId);
				final var entity = new CacheProjectGroup();
				entity.setProject(project);
				entity.setGroup(groups.get(groupId));
				em.persist(entity);
			}

			// Purge the old entries
			if (projectGroupIds != null) {
				projectGroupIds.remove(groupId);
				if (projectGroupIds.isEmpty()) {
					entities.remove(projectId);
				}
			}
		}

		// Remove old memberships
		entities.keySet().forEach(project ->
				entities.get(project).forEach(group -> {
					log.info("Deleting removed cache entry {}#{}-{}", CacheProjectGroup.class.getSimpleName(), project, group);
					em.createQuery("DELETE FROM CacheProjectGroup WHERE project.id=:project and group.id=:group")
							.setParameter("project", project)
							.setParameter("group", group)
							.executeUpdate();
				})
		);


		return allProjectGroup.size();
	}

	/**
	 * Remove a group from a group.
	 *
	 * @param subGroup the user to remove from the group
	 * @param group    the group to update.
	 */
	public void removeGroupFromGroup(final GroupOrg subGroup, final GroupOrg group) {
		em.createQuery("DELETE FROM CacheMembership WHERE subGroup.id=:subGroup AND group.id=:group")
				.setParameter("group", group.getId()).setParameter("subGroup", subGroup.getId()).executeUpdate();
	}

	/**
	 * Remove a user from a group.
	 *
	 * @param user  the user to remove from the group
	 * @param group the group to update.
	 */
	public void removeUserFromGroup(final UserOrg user, final GroupOrg group) {
		em.createQuery("DELETE FROM CacheMembership WHERE user.id=:user AND group.id=:group")
				.setParameter("group", group.getId()).setParameter("user", user.getId()).executeUpdate();
	}

	/**
	 * Reset the database cache with the provided groups/companies and users.
	 *
	 * @param users     All users.
	 * @param groups    All groups.
	 * @param companies All companies.
	 */
	public void reset(final Map<String, CompanyOrg> companies, final Map<String, GroupOrg> groups,
			final Map<String, UserOrg> users) {
		final var start = System.currentTimeMillis();

		// Insert data into database
		log.info("Updating cache entries: {} groups, {} companies, {} users", groups.size(), companies.size(), users.size());
		em.flush();
		em.clear();

		// Update companies
		final var oldCompanies = em.createQuery("FROM CacheCompany", CacheCompany.class)
				.getResultList().stream().collect(Collectors.toMap(CacheCompany::getId, Function.identity()));
		final var cacheCompanies = companies.values().stream().map(c -> create(c, oldCompanies))
				.collect(Collectors.toMap(CacheCompany::getId, Function.identity()));
		em.flush();

		// Update groups
		final var oldGroups = em.createQuery("FROM CacheGroup", CacheGroup.class)
				.getResultList().stream().collect(Collectors.toMap(CacheGroup::getId, Function.identity()));
		final var cacheGroups = groups.values().stream().map(c -> create(c, oldGroups))
				.collect(Collectors.toMap(CacheGroup::getId, Function.identity()));
		em.flush();

		final var memberships = persistUsersAndMemberships(users, groups, cacheGroups, cacheCompanies);
		em.flush();
		final var subscribedProjects = persistProjectGroups(cacheGroups);
		em.flush();
		final var updatedDelegate = updateDelegateDn(cacheGroups, cacheCompanies);
		em.flush();

		// Remove old groups and companies
		deleteOldCacheEntities(CacheGroup.class, oldGroups, groups, g -> {
					log.info("Deleting removed cache entry {}#{} (sub/groups)", CacheMembership.class.getSimpleName(), g);
			em.createQuery("DELETE FROM CacheMembership WHERE group.id=:group OR subGroup.id=:group")
					.setParameter("group", g)
					.executeUpdate();
			em.createQuery("DELETE FROM CacheProjectGroup WHERE group.id=:group")
					.setParameter("group", g)
					.executeUpdate();
				}
		);

		deleteOldCacheEntities(CacheCompany.class, oldCompanies, companies, null);
		em.flush();
		em.clear();

		log.info("Updated cache: {} groups, {} companies, {} users, {} memberships, {} project groups, {} updated delegates in {}",
				groups.size(), companies.size(), users.size(), memberships, subscribedProjects, updatedDelegate,
				DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));
		cacheRefreshTime = System.currentTimeMillis();
	}

	private <T extends Persistable<String>> void deleteOldCacheEntities(final Class<T> entityClass, final Map<String, T> oldEntities,
			final Map<String, ?> newEntities,
			Consumer<String> onDelete) {
		final var ids = oldEntities.keySet();
		ids.removeAll(newEntities.keySet());
		if (onDelete != null) {
			ids.forEach(onDelete);
		}
		ids.forEach(u -> {
			log.info("Deleting removed cache entry {}#{}", entityClass.getSimpleName(), u);
			em.createQuery("DELETE FROM " + entityClass.getSimpleName() + " WHERE id=:id")
					.setParameter("id", u)
					.executeUpdate();
		});
	}

	/**
	 * Transform company to JPA.
	 */
	private CacheCompany toCacheCompany(final CompanyOrg company, final CacheCompany entity) {
		return fillCacheContainer(company, entity);
	}

	/**
	 * Transform group to JPA.
	 */
	private CacheGroup toCacheGroup(final GroupOrg group, final CacheGroup entity) {
		return fillCacheContainer(group, entity);
	}

	/**
	 * Transform user to JPA.
	 */
	private CacheUser toCacheUser(final UserOrg user) {
		return toCacheUserInternal(user, new CacheUser(), null);
	}

	/**
	 * Transform user to JPA.
	 */
	private CacheUser toCacheUserInternal(final UserOrg user, final CacheUser entity, final Map<String, CacheCompany> companies) {
		entity.setId(user.getId());
		entity.setFirstName(user.getFirstName());
		entity.setLastName(user.getLastName());
		if (CollectionUtils.isNotEmpty(user.getMails())) {
			entity.setMails(user.getMails().get(0));
		}

		// Set the company if defined
		entity.setCompany(Optional.ofNullable(user.getCompany()).map(c -> {
			if (companies == null) {
				final var company = new CacheCompany();
				company.setId(c);
				return company;
			}
			return companies.get(c);
		}).orElse(null));
		return entity;
	}

	/**
	 * Update given user.
	 *
	 * @param user user to update.
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
	 * @param containers   The existing containers.
	 * @param receiverType The receiver type to update. And also the same type as the given containers.
	 * @param resourceType The delegate resource type to update. And also the same type as the given containers.
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
