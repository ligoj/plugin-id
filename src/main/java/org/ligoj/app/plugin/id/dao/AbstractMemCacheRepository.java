/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.util.EnumMap;
import java.util.Map;

import org.ligoj.app.api.Normalizer;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.ResourceOrg;
import org.ligoj.app.iam.UserOrg;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * In memory cache with JPA back-end cache for users/groups/companies.
 */
@Slf4j
public abstract class AbstractMemCacheRepository {

	/**
	 * Cache data type.
	 */
	public enum CacheDataType {
		/**
		 * Group type.
		 */
		GROUP,
		/**
		 * Company type.
		 */
		COMPANY,

		/**
		 * User type.
		 */
		USER
	}

	@Autowired
	@Setter
	protected IdCacheDao cache;

	/**
	 * IAM provider.
	 */
	@Autowired
	@Setter
	protected IamProvider[] iamProvider;

	/**
	 * Current data.
	 */
	protected Map<CacheDataType, Map<String, ? extends ResourceOrg>> data;

	/**
	 * Add the group to the given group.Cache is also updated.
	 *
	 * @param subGroup The group to add to the other group.
	 * @param group    The group to update.
	 */
	public void addGroupToGroup(final GroupOrg subGroup, final GroupOrg group) {

		// Add to JPA cache
		cache.addGroupToGroup(subGroup, group);

		// Also update the membership cache
		group.getSubGroups().add(subGroup.getId());
		subGroup.getGroups().add(group.getId());
	}

	/**
	 * Add the user to the given group.Cache is also updated.
	 *
	 * @param user  The user to add to the other group.
	 * @param group The group to update.
	 */
	public void addUserToGroup(final UserOrg user, final GroupOrg group) {

		// Add to JPA cache
		cache.addUserToGroup(user, group);

		// Also update the membership cache
		group.getMembers().add(user.getId());
		user.getGroups().add(group.getId());
	}

	/**
	 * Add given company to the cache.
	 *
	 * @param company The new company.
	 * @return <code>company</code>
	 */
	public CompanyOrg create(final CompanyOrg company) {
		cache.create(company);
		getCompany().findAll().put(company.getId(), company);
		return company;
	}

	/**
	 * Add given group to the cache.
	 *
	 * @param group The new group.
	 * @return <code>group</code>
	 */
	public GroupOrg create(final GroupOrg group) {
		cache.create(group);
		getGroup().findAll().put(group.getId(), group);
		return group;
	}

	/**
	 * Add given user to the cache. Membership is not considered.
	 *
	 * @param user The new user.
	 * @return <code>user</code>
	 */
	public UserOrg create(final UserOrg user) {
		cache.create(user);
		getUser().findAll().put(user.getId(), user);
		return user;
	}

	/**
	 * Remove given company from the cache. Warning, it is assumed there is no more user associated to the deleted
	 * company.
	 *
	 * @param company The company to remove.
	 */
	public void delete(final CompanyOrg company) {
		final Map<String, CompanyOrg> companiesNameToDn = getCompany().findAll();

		// Remove from JPA cache
		cache.delete(company);

		// Remove from in-memory cache
		companiesNameToDn.remove(company.getId());
	}

	/**
	 * Remove given group from the cache.
	 *
	 * @param group the group to remove.
	 */
	public void delete(final GroupOrg group) {
		final Map<String, GroupOrg> groupsNameToDn = getGroup().findAll();

		// Remove the group from the users
		deleteMemoryAssociations(group, getUser().findAll());

		// Remove from JPA cache
		cache.delete(group);

		// Remove the group
		groupsNameToDn.remove(group.getId());
	}

	/**
	 * Remove given group from the cache. User should have been removed from each group before that, this function does
	 * not update in memory membership.
	 *
	 * @param user the user to remove.
	 */
	public void delete(final UserOrg user) {
		final Map<String, UserOrg> users = getUser().findAll();

		// Remove from JPA cache
		cache.delete(user);

		// Remove it-self from in-memory cache
		users.remove(Normalizer.normalize(user.getId()));
	}

	/**
	 * Remove all users from the given group and empty the group.
	 *
	 * @param group The group to empty.
	 * @param users All known users.
	 */
	private void deleteMemoryAssociations(final GroupOrg group, final Map<String, UserOrg> users) {
		// Remove from in-memory cache all users
		for (final String member : group.getMembers()) {
			users.get(member).getGroups().remove(group.getId());
		}

		// Clear the members list
		group.getMembers().clear();
	}

	/**
	 * Remove all users from the given group and empty the group.
	 *
	 * @param group The group to empty.
	 * @param users All known users could be removed from this group.
	 */
	public void empty(final GroupOrg group, final Map<String, UserOrg> users) {
		// Remove the group from the users
		deleteMemoryAssociations(group, users);

		// Remove memberships from JPA cache
		cache.empty(group);
	}

	/**
	 * Company repository provider.
	 *
	 * @return Company repository provider.
	 */
	protected ICompanyRepository getCompany() {
		return iamProvider[0].getConfiguration().getCompanyRepository();
	}

	/**
	 * Reset the database cache with the LDAP data. Note there is no synchronization for this method. Initial first
	 * concurrent calls may note involve the cache.
	 *
	 * @return The cached LDAP data..
	 */
	public abstract Map<CacheDataType, Map<String, ? extends ResourceOrg>> getData();

	/**
	 * Group repository provider.
	 *
	 * @return Group repository provider.
	 */
	protected IGroupRepository getGroup() {
		return iamProvider[0].getConfiguration().getGroupRepository();
	}

	/**
	 * User repository provider.
	 *
	 * @return User repository provider.
	 */
	protected IUserRepository getUser() {
		return iamProvider[0].getConfiguration().getUserRepository();
	}

	/**
	 * Reset the database cache with the LDAP data. Note there is no synchronization for this method. Initial first
	 * concurrent calls may note involve the cache.
	 *
	 * @return The fresh LDAP data..
	 */
	protected Map<CacheDataType, Map<String, ? extends ResourceOrg>> refreshData() {
		final Map<CacheDataType, Map<String, ? extends ResourceOrg>> result = new EnumMap<>(CacheDataType.class);

		// Fetch origin data
		log.info("Fetching origin data ...");
		final Map<String, GroupOrg> groups = getGroup().findAllNoCache();
		result.put(CacheDataType.COMPANY, getCompany().findAllNoCache());
		result.put(CacheDataType.GROUP, groups);
		result.put(CacheDataType.USER, getUser().findAllNoCache(groups));
		this.data = result;
		return result;
	}

	/**
	 * Remove the group from the another group. Cache is also updated but only in group members.
	 *
	 * @param subGroup The group to remove from the other group.
	 * @param group    The group to update.
	 */
	public void removeGroupFromGroup(final GroupOrg subGroup, final GroupOrg group) {
		// Remove from JPA cache
		cache.removeGroupFromGroup(subGroup, group);

		// Also update the membership cache
		group.getSubGroups().remove(subGroup.getId());
		subGroup.getGroups().remove(group.getId());
	}

	/**
	 * Remove the user from the given group. Cache is also updated but only in group members.
	 *
	 * @param user  The user to remove from the given group.
	 * @param group The group to update.
	 */
	public void removeUserFromGroup(final UserOrg user, final GroupOrg group) {
		// Remove from JPA cache
		cache.removeUserFromGroup(user, group);

		// Also update the membership cache
		user.getGroups().remove(group.getId());
		group.getMembers().remove(user.getId());
	}

	/**
	 * Update the attributes.
	 *
	 * @param user The user to update.
	 */
	public void update(final UserOrg user) {
		cache.update(user);
	}
}
