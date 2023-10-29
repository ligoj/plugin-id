/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;

import java.util.Map;

/**
 * Cache synchronization from SQL cache to database.
 */
public interface IdCacheDao {

	/**
	 * Attribute name used as filter and path "group".
	 */
	String GROUP_ATTRIBUTE = "group";
	/**
	 * Attribute name used as filter and path "user".
	 */
	String USER_ATTRIBUTE = "user";

	/**
	 * Add a group to a group.
	 *
	 * @param subGroup the group to add the parent group.
	 * @param group    the group to update.
	 */
	void addGroupToGroup(GroupOrg subGroup, GroupOrg group);

	/**
	 * Add a user to a group.
	 *
	 * @param user  the user to add to the group.
	 * @param group the group to update.
	 */
	void addUserToGroup(UserOrg user, GroupOrg group);


	/**
	 * Persist a new company and flush.
	 *
	 * @param company  the company to persist.
	 * @param entities the existing entities by ID.
	 * @return The persisted {@link CacheCompany}
	 */
	CacheCompany create(CompanyOrg company, Map<String, CacheCompany> entities);

	/**
	 * Persist a new company and flush.
	 *
	 * @param company the company to persist.
	 * @return The persisted {@link CacheCompany}
	 */
	CacheCompany create(CompanyOrg company);

	/**
	 * Persist a new group and flush.
	 *
	 * @param group    the group to persist.
	 * @param entities the existing entities by ID.
	 * @return The persisted {@link CacheGroup}
	 */
	CacheGroup create(GroupOrg group, Map<String, CacheGroup> entities);

	/**
	 * Persist a new user and flush
	 *
	 * @param user the user to persist.
	 */
	void create(UserOrg user);

	/**
	 * Delete a company. Warning, it is assumed there is no more user associated to the deleted company.
	 *
	 * @param company the company to delete.
	 */
	void delete(CompanyOrg company);

	/**
	 * Delete a group.
	 *
	 * @param group the group to delete.
	 */
	void delete(GroupOrg group);

	/**
	 * Delete a user.
	 *
	 * @param user the user to delete.
	 */
	void delete(UserOrg user);

	/**
	 * Remove all user membership to this group. Subgroups are not removed.
	 *
	 * @param group the group to empty.
	 */
	void empty(GroupOrg group);

	/**
	 * Remove a group from a group.
	 *
	 * @param subGroup the user to remove from the group
	 * @param group    the group to update.
	 */
	void removeGroupFromGroup(GroupOrg subGroup, GroupOrg group);

	/**
	 * Remove a user from a group.
	 *
	 * @param user  the user to remove from the group
	 * @param group the group to update.
	 */
	void removeUserFromGroup(UserOrg user, GroupOrg group);

	/**
	 * Reset the database cache with the provided groups/companies and users.
	 *
	 * @param users     All users.
	 * @param groups    All groups.
	 * @param companies All companies.
	 */
	void reset(Map<String, CompanyOrg> companies, Map<String, GroupOrg> groups, Map<String, UserOrg> users);

	/**
	 * Update given user.
	 *
	 * @param user user to update.
	 */
	void update(UserOrg user);

	/**
	 * Return the UTC last cache refresh time (milli).
	 * @return the UTC last cache refresh time (milli).
	 */
	long getCacheRefreshTime();
}
