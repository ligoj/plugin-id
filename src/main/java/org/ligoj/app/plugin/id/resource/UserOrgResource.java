/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.apache.cxf.jaxrs.impl.UriInfoImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.ligoj.app.api.Normalizer;
import org.ligoj.app.iam.*;
import org.ligoj.app.iam.dao.DelegateOrgRepository;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.app.plugin.id.DnUtils;
import org.ligoj.app.plugin.id.dao.PasswordResetAuditRepository;
import org.ligoj.app.plugin.id.model.PasswordResetAudit;
import org.ligoj.bootstrap.core.json.PaginationJson;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.ligoj.bootstrap.resource.system.configuration.ConfigurationResource;
import org.ligoj.bootstrap.resource.system.session.ISessionSettingsProvider;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * User resource.
 */
@Path(IdentityResource.SERVICE_URL + "/user")
@Service
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
@Transactional
public class UserOrgResource extends AbstractOrgResource implements ISessionSettingsProvider {

	/**
	 * Message key for read only resource : no "write" right.
	 */
	private static final String READ_ONLY = "read-only";

	/**
	 * Name of "group" attribute.
	 */
	private static final String GROUP = "group";

	/**
	 * The primary business key
	 */
	public static final String USER_KEY = "id";

	@Autowired
	private DelegateOrgRepository delegateRepository;
	@Autowired
	private ConfigurationResource configuration;

	@Autowired
	private PasswordResetAuditRepository passwordResetRepository;

	@Autowired
	private PaginationJson paginationJson;

	@Autowired
	protected CompanyResource companyResource;

	@Autowired
	protected GroupResource groupResource;

	@Autowired
	protected ApplicationContext applicationContext;

	/**
	 * Ordered columns.
	 */
	private static final Map<String, String> ORDERED_COLUMNS = new HashMap<>();

	static {
		ORDERED_COLUMNS.put(USER_KEY, USER_KEY);
		ORDERED_COLUMNS.put("firstName", "firstName");
		ORDERED_COLUMNS.put("lastName", "lastName");
		ORDERED_COLUMNS.put("mails", "mail");
		ORDERED_COLUMNS.put(SimpleUser.COMPANY_ALIAS, SimpleUser.COMPANY_ALIAS);
	}

	/**
	 * Return users matching the given criteria. The visible groups, trees and companies are checked. The returned
	 * groups of each user depends on the groups the user can see. The result is not secured : it contains DN.
	 *
	 * @param company The optional company name to match.
	 * @param group   The optional group name to match.
	 * @return All matched unsorted users.
	 */
	public List<UserOrg> findAllNotSecure(final String company, final String group) {
		final var visibleGroups = groupResource.getContainers();

		// Search the users
		final var message = new MessageImpl();
		message.put(Message.QUERY_STRING, DataTableAttributes.PAGE_LENGTH + "=10000000");
		return findAllNotSecure(visibleGroups, company, group, null, new UriInfoImpl(message)).getContent();
	}

	/**
	 * Return users matching the given criteria. The visible groups, trees and companies are checked. The returned
	 * groups of each user depends on the groups the user can see and are in normalized CN form. The result is not
	 * secured, it contains DN.
	 *
	 * @param visibleGroups The visible groups by the principal user.
	 * @param company       the optional company name to match. Will be normalized.
	 * @param group         the optional group name to match. May be <code>null</code>.
	 * @param criteria      the optional criteria to match.
	 * @param uriInfo       filter data.
	 * @return found users.
	 */
	private Page<UserOrg> findAllNotSecure(final Set<GroupOrg> visibleGroups, final String company, final String group, final String criteria, @Context final UriInfo uriInfo) {
		final var pageRequest = paginationJson.getPageRequest(uriInfo, ORDERED_COLUMNS);
		final var visibleCompanies = companyResource.getContainers().stream().map(CompanyOrg::getId).collect(Collectors.toSet());
		final var allGroups = getGroupRepository().findAll();

		// The companies to use
		final var filteredCompanies = computeFilteredCompanies(Normalizer.normalize(company), visibleCompanies);

		// The groups to use
		final var filteredGroups = group == null ? null : computeFilteredGroups(group, visibleGroups, allGroups);

		// Search the users
		return getUserRepository().findAll(filteredGroups, filteredCompanies, StringUtils.trimToNull(criteria), pageRequest);
	}

	/**
	 * Return users matching the given criteria. The visible groups, trees and companies are checked. The returned
	 * groups of each user depends on the groups the user can see/write, and are in CN form.
	 *
	 * @param company  the optional company name to match.
	 * @param group    the optional group name to match.
	 * @param criteria the optional criteria to match.
	 * @param uriInfo  filter data.
	 * @return found users.
	 */
	@GET
	public TableItem<UserOrgVo> findAll(@QueryParam(SimpleUser.COMPANY_ALIAS) final String company, @QueryParam(GROUP) final String group, @QueryParam(DataTableAttributes.SEARCH) final String criteria, @Context final UriInfo uriInfo) {
		final var visibleGroups = groupResource.getContainers();
		final var writableGroups = groupResource.getContainersForWrite();
		final var writableCompanies = companyResource.getContainersForWrite();
		final var writableCompaniesIds = writableCompanies.stream().map(CompanyOrg::getId).collect(Collectors.toSet());

		// Search the users
		final var findAll = findAllNotSecure(visibleGroups, company, group, criteria, uriInfo);

		// Apply pagination and secure the users data
		final var result = paginationJson.applyPagination(uriInfo, findAll, rawUserOrg -> {

			final var securedUserOrg = new UserOrgVo();
			rawUserOrg.copy(securedUserOrg);
			securedUserOrg.setCanWrite(writableCompaniesIds.contains(rawUserOrg.getCompany()));
			securedUserOrg.setCanWriteGroups(!writableGroups.isEmpty());

			// Show only the groups that are also visible to current user
			securedUserOrg.setGroups(visibleGroups.stream().filter(mGroup -> rawUserOrg.getGroups().contains(mGroup.getId())).map(mGroup -> {
				final var vo = new GroupVo();
				vo.setCanWrite(writableGroups.contains(mGroup));
				vo.setName(mGroup.getName());
				return vo;
			}).toList());
			return securedUserOrg;
		});

		// Forward custom attributes definition
		result.setExtensions(Map.of("customAttributes", getUserRepository().getCustomAttributes()));

		return result;
	}

	/**
	 * Return an intersection of given set of visible companies and the optional requested company.
	 */
	private Set<String> computeFilteredCompanies(final String requestedCompany, final Collection<String> visibleCompanies) {
		// Restrict access to visible companies
		final Set<String> filteredCompanies;
		if (StringUtils.isBlank(requestedCompany)) {
			// No requested company, use all of them
			filteredCompanies = new HashSet<>(visibleCompanies);
		} else if (visibleCompanies.contains(requestedCompany)) {
			// Requested company is visible, return it
			filteredCompanies = Collections.singleton(requestedCompany);
		} else {
			// Requested company does not exist, result would be an empty list
			filteredCompanies = Collections.emptySet();
		}

		return filteredCompanies;
	}

	/**
	 * Computed visible groups.
	 */
	private List<GroupOrg> computeFilteredGroups(final String group, final Set<GroupOrg> visibleGroups, final Map<String, GroupOrg> allGroups) {
		// Restrict access to delegated groups
		return Optional.ofNullable(allGroups.get(Normalizer.normalize(group))).map(fg -> allGroups.values().stream().filter(visibleGroups::contains)
				// Filter the group, including the children
				.filter(g -> DnUtils.equalsOrParentOf(fg.getDn(), g.getDn())).toList()).orElse(Collections.emptyList());
	}

	/**
	 * Return a specific user from his/her login. When user does not exist or is within a non-visible company, return a
	 * 404.
	 *
	 * @param user The user to find. A normalized form will be used for the search.
	 * @return found user. Never <code>null</code>.
	 */
	@GET
	@Path("{user:" + SimpleUser.USER_PATTERN + "}")
	public UserOrg findById(@PathParam("user") final String user) {
		final var rawUserOrg = getUserRepository().findByIdExpected(securityHelper.getLogin(), Normalizer.normalize(user));

		// Check if the user lock status without using cache
		getUserRepository().checkLockStatus(rawUserOrg);

		// User has been found, secure the object regarding the visible groups
		final var securedUserOrg = new UserOrg();
		rawUserOrg.copy(securedUserOrg);

		// Show only the groups of user that are also visible to current user
		final var visibleGroups = groupResource.getContainers();
		securedUserOrg.setGroups(visibleGroups.stream().filter(mGroup -> rawUserOrg.getGroups().contains(mGroup.getId())).sorted().map(GroupOrg::getName).toList());
		return securedUserOrg;
	}

	/**
	 * Add given user to a group.
	 *
	 * @param user  The user to add.
	 * @param group The group to update.
	 */
	@PUT
	@Path("{user}/group/{group}")
	public void addUserToGroup(@PathParam("user") final String user, @PathParam(GROUP) final String group) {
		updateGroupUser(user, Normalizer.normalize(group), Collection::add);
	}

	/**
	 * Remove given user from a group.
	 *
	 * @param user  The user to remove.
	 * @param group The group to update.
	 */
	@DELETE
	@Path("{user}/group/{group}")
	public void removeUserFromGroup(@PathParam("user") final String user, @PathParam(GROUP) final String group) {
		updateGroupUser(user, Normalizer.normalize(group), Collection::remove);
	}

	/**
	 * Performs an operation on a group for a user.
	 *
	 * @param user    The user to move.
	 * @param group   The group to update.
	 * @param updater The function to execute on computed groups regarding the given user: add or remove
	 */
	private void updateGroupUser(final String user, final String group, final BiPredicate<Collection<String>, String> updater) {

		// Get all delegates of current user
		final var delegates = delegateRepository.findAllByUser(securityHelper.getLogin());

		// Get the implied user
		final var userOrg = getUserRepository().findByIdExpected(user);

		// Check the implied group
		if (getGroupRepository().findById(securityHelper.getLogin(), group) == null) {
			throw new ValidationJsonException(GROUP, "not-exist", "0", GROUP, "1", group);
		}
		validateWriteGroup(group, delegates);

		// Compute the new groups
		final var newGroups = new HashSet<>(userOrg.getGroups());
		if (updater.test(newGroups, group)) {

			// Replace the user groups by the normalized groups including the
			// one we have just updated
			final var mergedGroups = mergeGroups(delegates, userOrg, newGroups);

			// Update membership
			getUserRepository().updateMembership(new ArrayList<>(mergedGroups), userOrg);
		}
	}

	/**
	 * Update the given user.
	 *
	 * @param user The user definition, and associated groups. Group changes  and user attributes changes are
	 *             checked.
	 * @return the update attributes and related changes. Currently only `groups` attributes is supported and contains only the
	 */
	@PUT
	public UserUpdateResult update(final UserOrgEditionVo user) {
		// Check the right on the company and the groups
		final var hasAttributeChange = validateChanges(securityHelper.getLogin(), user);

		// Check the user exists
		getUserRepository().findByIdExpected(user.getId());

		return saveOrUpdate(user, hasAttributeChange);
	}

	/**
	 * Create the given user.
	 *
	 * @param user  The user definition, and associated groups. Initial groups are checked.User definition is checked.
	 * @param quiet Flag to turn off the possible notification such as mail.
	 * @return The generated password if was expected in the user definition.
	 */
	public String create(final UserOrgEditionVo user, final boolean quiet) {
		// Check the right on the company and the groups
		validateChanges(securityHelper.getLogin(), user);

		// Check the user does not exist
		if (getUserRepository().findById(user.getId()) != null) {
			throw new ValidationJsonException(USER_KEY, "already-exist", "0", USER_KEY, "1", user.getId());
		}

		saveOrUpdate(user, quiet, true);
		return user.getGeneratedPassword();
	}

	/**
	 * Create the given user.
	 *
	 * @param user The user definition, and associated groups. Initial groups are checked.User definition is checked.
	 * @return The generated password if was expected in the user definition.
	 */
	@POST
	public String create(final UserOrgEditionVo user) {
		return create(user, false);
	}

	/**
	 * Validate the user changes regarding the current user's right, replace group names with the exact CN, and replace
	 * the company with a normalized one.<br>
	 * Rules, order is important :
	 * <ul>
	 * <li>At least one valid delegate must exist (valid or not against the involved user). If not, act as if the
	 * company does not exist.</li>
	 * <li>Involved company must exist</li>
	 * <li>Involved company must be visible by the principal user. If not at if it does not exist, one</li>
	 * <li>Involved company must be writable by the principal user when there is one updated attribute. Otherwise
	 * indicate the read-only state.</li>
	 * <li>Involved groups must exist</li>
	 * <li>Involved groups must be visible by the current user, if not, act as if it does not exist. So the user can
	 * only involve visible groups he/she. These groups are completed with the other invisible groups the user may
	 * already have.</li>
	 * <li>Involved changed groups must writable by the principal user. Otherwise indicate the read-only state.</li>
	 * </ul>
	 */
	private boolean validateChanges(final String principal, final UserOrgEditionVo importEntry) {
		// First cleanup the entry
		normalize(importEntry);

		// Get all delegates of current user
		final var delegates = delegateRepository.findAllByUser(principal);

		// Get the stored data of the implied user
		final var userOrg = getUserRepository().findById(importEntry.getId());

		// Check the implied company and request changes
		final var cleanCompany = Normalizer.normalize(importEntry.getCompany());
		final var companyDn = getCompanyRepository().findByIdExpected(principal, cleanCompany).getDn();
		final var hasAttributeChange = hasAttributeChange(importEntry, userOrg);
		if (hasAttributeChange && !canWrite(delegates, companyDn, DelegateType.COMPANY)) {
			// Visible but without write access
			log.info("Attempt to create/update a read-only user '{}', company '{}'", importEntry.getId(), cleanCompany);
			throw new ValidationJsonException(SimpleUser.COMPANY_ALIAS, READ_ONLY, "0", SimpleUser.COMPANY_ALIAS, "1", importEntry.getCompany());
		}

		// Replace with the normalized company
		importEntry.setCompany(cleanCompany);

		// Check the groups : one group not writable implies entry creation to fail
		validateAndGroupsCN(userOrg, importEntry, delegates);

		// Replace the user groups by the normalized groups including the ones the user does not see
		if (userOrg != null) {
			// Check the company change
			if (!userOrg.getCompany().equals(importEntry.getCompany())) {
				// Check the user can be removed from the old company
				checkDeletionRight(importEntry.getId(), "move");
			}

			// Compute merged group identifiers
			importEntry.setGroups(new ArrayList<>(mergeGroups(delegates, userOrg, importEntry.getGroups())));
		}
		return hasAttributeChange;
	}

	/**
	 * Validate assigned groups, department and return corresponding group identifiers.
	 *
	 * @param userOrg     The user to update.
	 * @param importEntry The user raw values to update.
	 * @param delegates   The delegates (read/write) of the principal user.
	 */
	private void validateAndGroupsCN(final UserOrg userOrg, final UserOrgEditionVo importEntry, final List<DelegateOrg> delegates) {

		// First complete the groups with the implicit ones from department
		final var previous = Optional.ofNullable(userOrg).map(UserOrg::getDepartment).orElse(null);
		if (ObjectUtils.notEqual(previous, importEntry.getDepartment())) {
			Optional.ofNullable(toDepartmentGroup(previous)).map(GroupOrg::getId).ifPresent(importEntry.getGroups()::remove);
			Optional.ofNullable(toDepartmentGroup(importEntry.getDepartment())).map(GroupOrg::getId).ifPresent(importEntry.getGroups()::add);
		}
		validateAndGroupsCN(Optional.ofNullable(userOrg).map(UserOrg::getGroups).orElse(Collections.emptyList()), importEntry.getGroups(), delegates);
	}

	/**
	 * Validate assigned groups, and return corresponding group identifiers. The groups must be visible by the
	 * principal, and added/removed groups from the user must be writable by the principal.
	 *
	 * @param previousGroups The current user's groups used to validate the changes.
	 * @param desiredGroups  The groups the principal user has assigned to the user. In this list, there are some
	 *                       read-only groups previously assigned to this user. Only the changes are checked.
	 * @param delegates      The delegates (read/write) of the principal user.
	 */
	private void validateAndGroupsCN(final Collection<String> previousGroups, final Collection<String> desiredGroups, final List<DelegateOrg> delegates) {
		// Check visibility of the desired groups
		final var repository = getGroupRepository();
		desiredGroups.forEach(g -> repository.findByIdExpected(securityHelper.getLogin(), g));

		// Check the visible updated groups can be edited by the principal
		CollectionUtils.disjunction(desiredGroups, previousGroups).forEach(g -> validateWriteGroup(g, delegates));
	}

	/**
	 * Validate a change of membership of given group by the principal user.
	 *
	 * @param updatedGroup The group the principal user is updating : add/remove a user. The visibility of this must
	 *                     have been previously checked.
	 * @param delegates    The delegates (read/write) of the principal user.
	 */
	private void validateWriteGroup(final String updatedGroup, final List<DelegateOrg> delegates) {

		// Check the visible updated groups can be edited by the principal
		Optional.ofNullable(getGroupRepository().findById(securityHelper.getLogin(), updatedGroup)).filter(g -> !canWrite(delegates, g.getDn(), DelegateType.GROUP)).ifPresent(g -> {
			throw new ValidationJsonException(GROUP, READ_ONLY, "0", GROUP, "1", g.getId());
		});
	}

	/**
	 * Merge user groups with this formula :
	 * <ul>
	 * <li>DG :Desired groups by current user, and to be set to the entry. These groups must have been previously
	 * checked regarding against the rights the current user has on these groups. So are visible for the principal
	 * user</li>
	 * <li>CG : Current groups of internal entry</li>
	 * <li>VG : Visible groups in CG</li>
	 * <li>WG : Writable groups in VG</li>
	 * <li>GG : Final groups of entry = CG-WG+DG</li>
	 * </ul>
	 *
	 * @param delegates the available delegates of current principal user.
	 * @param userOrg   The internal user entry to update.
	 * @param groups    The writable groups identifiers to be set to the user in addition of the non-visible or writable
	 *                  groups by the current principal user.
	 * @return the merged group identifiers to be set internally.
	 */
	private Collection<String> mergeGroups(final List<DelegateOrg> delegates, final UserOrg userOrg, final Collection<String> groups) {
		// Compute the groups merged groups
		final Collection<String> newGroups = new HashSet<>(userOrg.getGroups());
		newGroups.addAll(groups);
		final var repository = getGroupRepository();
		for (final var oldGroup : userOrg.getGroups()) {
			final var oldGroupDn = repository.findById(oldGroup).getDn();
			if (!groups.contains(oldGroup) && canWrite(delegates, oldGroupDn, DelegateType.GROUP)) {
				// This group is writable, so it has been explicitly removed by the current user
				newGroups.remove(oldGroup);
			}
		}
		return newGroups;
	}

	/**
	 * Normalize the entry : capitalize and trimming.
	 */
	private void normalize(final UserOrgEditionVo importEntry) {
		// Normalize the identifiers
		importEntry.setCompany(Normalizer.normalize(importEntry.getCompany()));
		importEntry.setId(StringUtils.trimToNull(Normalizer.normalize(importEntry.getId())));
		importEntry.setGroups(new ArrayList<>(Normalizer.normalize(importEntry.getGroups())));

		// Fix the names of user
		importEntry.setDepartment(StringUtils.trimToNull(importEntry.getDepartment()));
		importEntry.setLocalId(StringUtils.trimToNull(importEntry.getLocalId()));
		importEntry.setLastName(WordUtils.capitalizeFully(StringUtils.trimToNull(importEntry.getLastName())));
		importEntry.setFirstName(WordUtils.capitalizeFully(StringUtils.trimToNull(importEntry.getFirstName())));
	}

	private boolean canWrite(final List<DelegateOrg> delegates, final String dn, final DelegateType type) {
		return resource.isAdmin(securityHelper.getLogin()) || delegates.stream().anyMatch(delegate -> canWrite(delegate, dn, type));
	}

	protected boolean canWrite(final DelegateOrg delegate, final String dn, final DelegateType type) {
		return (delegate.getType() == type || delegate.getType() == DelegateType.TREE) && delegate.isCanWrite() && DnUtils.equalsOrParentOf(delegate.getDn(), dn);
	}

	/**
	 * Convert a map to a comparable string with sorted keys.
	 */
	String mapToString(Map<String, String> map) {
		if (map == null) {
			return "";
		}
		return map.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Object::toString).collect(Collectors.joining());
	}

	/**
	 * Indicates the two user details have attribute differences.
	 */
	boolean hasAttributeChange(final UserOrgEditionVo importEntry, final UserOrg userOrg) {
		return hasAttributeChange(importEntry, userOrg == null, "new")
				|| hasAttributeChange(importEntry, userOrg, SimpleUser::getFirstName, SimpleUser::getLastName, SimpleUser::getCompany, SimpleUser::getLocalId, SimpleUser::getDepartment)
				|| hasAttributeChange(importEntry, !mapToString(importEntry.getCustomAttributes()).equals(mapToString(userOrg.getCustomAttributes())), "customAttributes")
				|| hasAttributeChange(importEntry, !userOrg.getMails().contains(importEntry.getMail()), "mail");
	}

	private boolean hasAttributeChange(final SimpleUser importEntry, boolean hasChange, String source) {
		if (hasChange) {
			log.info("At least one change required for user {}, source: {}", importEntry.getId(), source);
		}
		return hasChange;
	}

	/**
	 * Indicate the two user details have attribute differences
	 */
	@SafeVarargs
	private boolean hasAttributeChange(final SimpleUser user1, final SimpleUser user2, final Function<SimpleUser, String>... equals) {
		final var predicateFalse = Arrays.stream(equals).filter(f -> !StringUtils.equalsIgnoreCase(StringUtils.trimToNull(f.apply(user2)), StringUtils.trimToNull(f.apply(user1)))).findFirst().orElse(null);
		return predicateFalse != null && hasAttributeChange(user1, true, String.format("'%s' != '%s'", predicateFalse.apply(user1), predicateFalse.apply(user2)));
	}

	/**
	 * Create the user is not exist and update the related groups and company.<br>
	 * The mail of the entry will replace the one of the repository if it does not contain any mail. If entry did
	 * not exist or, if there was no password (or a dummy one), it will be set to the one of import of a new generated
	 * password. <br>
	 * When mail or password is updated a mail is sent to the user with the account, and eventually the new
	 * password.<br>
	 * Groups of entry will be normalized.
	 *
	 * @param importEntry        The entry to save or to update.
	 * @param quiet              Flag to turn off the possible notification such as mail.
	 * @param hasAttributeChange When <code>false</code>, underlying user database will not be updated, only membership as needed.
	 * @return the updated attributes and related changes. Currently only `groups` attributes is supported and contains only the
	 */
	private UserUpdateResult saveOrUpdate(final UserOrgEditionVo importEntry, final boolean quiet, final boolean hasAttributeChange) {

		// Create as needed the user, groups will be proceeded after.
		final var repository = getUserRepository();
		var user = repository.findById(importEntry.getId());
		final var newUser = toUserOrg(importEntry);
		if (user == null) {
			// Create a new entry in repository
			log.info("{} will be created", newUser.getId());
			user = repository.create(newUser);

			// Set the password
			final var generatedPassword = updatePassword(newUser, quiet);
			if (importEntry.isReturnGeneratePassword()) {
				importEntry.setGeneratedPassword(generatedPassword);
			}
		} else if (hasAttributeChange) {
			updateUser(user, newUser, quiet);
		}

		// Update membership
		return repository.updateMembership(importEntry.getGroups(), user);
	}

	/**
	 * Create the user is not exist and update the related groups and company.<br>
	 * The mail of the entry will replace the one of the repository if it does not contain any mail. If entry did
	 * not exist or, if there was no password (or a dummy one), it will be set to the one of import of a new generated
	 * password. <br>
	 * When mail or password is updated a mail is sent to the user with the account, and eventually the new
	 * password.<br>
	 * Groups of entry will be normalized.
	 *
	 * @param importEntry        The entry to save or to update.
	 * @param hasAttributeChange When <code>false</code>, underlying user database will not be updated, only membership as needed.
	 * @return the update attributes and related changes. Currently only `groups` attributes is supported and contains only the
	 */
	public UserUpdateResult saveOrUpdate(final UserOrgEditionVo importEntry, final boolean hasAttributeChange) {
		return saveOrUpdate(importEntry, false, hasAttributeChange);
	}

	/**
	 * Update the attributes the given user. Groups are not managed there.
	 */
	private void updateUser(final UserOrg oldUser, final UserOrg newUser, final boolean quiet) {
		log.info("{} already exists", newUser.getId());

		// First update the DN
		newUser.setDn(getUserRepository().toDn(newUser));
		updateCompanyAsNeeded(oldUser, newUser);

		// Then, update the unsecured attributes : first name, etc.
		final var hadNoMail = oldUser.getMails().isEmpty();
		getUserRepository().updateUser(newUser);

		// Then update the mail and/or password
		if (newUser.getMails().isEmpty()) {
			// No mail, no notification
			log.info("{} already exists, but has no mail", newUser.getId());
		} else if (hadNoMail) {
			// Mail has been added, set a new password
			log.info("{} already exists, but a mail has been created", newUser.getId());
			updatePassword(newUser, quiet);
		} else if (!oldUser.isSecured()) {
			// Override the password
			log.info("{} had no password, a mail will be sent", newUser.getId());
			updatePassword(newUser, quiet);
		}

	}

	/**
	 * Convert the import format to the internal format.
	 *
	 * @param importEntry The raw imported user.
	 * @return The internal format of the user.
	 */
	private UserOrg toUserOrg(final UserOrgEditionVo importEntry) {
		final var user = new UserOrg();
		importEntry.copy(user);
		user.setGroups(new ArrayList<>());
		final List<String> mails = new ArrayList<>();
		CollectionUtils.addIgnoreNull(mails, importEntry.getMail());
		user.setMails(mails);
		return user;
	}

	/**
	 * Delete a user.<br>
	 * Rules, order is important :
	 * <ul>
	 * <li>Only users managing the company of this user can perform the deletion, if not, act as if the user did not
	 * exist</li>
	 * <li>User must exist</li>
	 * </ul>
	 * Note : even if the user requesting this deletion has no right on the groups the involved user, this operation can
	 * be performed.
	 *
	 * @param user The user to delete. A normalized form of this parameter will be used for this operation.
	 */
	@DELETE
	@Path("{user}")
	public void delete(@PathParam("user") final String user) {
		// Check the user can be deleted
		final var userOrg = checkDeletionRight(user, "delete");

		// Hard deletion
		// Check the group : You can't delete a user if he is the last member
		// of a group
		final var allGroups = getGroupRepository().findAll();
		checkLastMemberInGroups(userOrg, allGroups);

		final var repository = getUserRepository();
		// Revoke all memberships of this user
		repository.updateMembership(new ArrayList<>(), userOrg);

		repository.delete(userOrg);
	}

	/**
	 * Disable a user. The user's password is cleared (empty) and a flag is added to tag this user as locked to prevent
	 * further password reset. Other properties are untouched.<br>
	 * Rules, order is important :
	 * <ul>
	 * <li>Only users managing the company of this user can perform the lock, if not, act as if the user did not
	 * exist</li>
	 * <li>User must exist</li>
	 * </ul>
	 * Note : even if the user requesting this operation has no right on the groups of the involved user, this operation
	 * can be performed.
	 *
	 * @param user The user to lock. A normalized form of this parameter will be used for this operation.
	 */
	@DELETE
	@Path("{user}/lock")
	public void lock(@PathParam("user") final String user) {
		getUserRepository().lock(securityHelper.getLogin(), checkDeletionRight(user, "lock"));
	}

	/**
	 * Isolate a user. The user is locked and also is moved to a different location from the user repository. This move
	 * ensures the tools lose sight of this user. Usually the target location is outside the scope/branch of users the other
	 * tools are watching.<br>
	 * All memberships are updated, the user's DN is changed, all groups must be updated. Rules, order is important :
	 * <ul>
	 * <li>Only users managing the company of this user can perform the disable, if not, act as if the user did not
	 * exist</li>
	 * <li>User must exist</li>
	 * </ul>
	 * Note : even if the user requesting this operation has no right on the groups the involved user, this operation
	 * can be performed.
	 *
	 * @param user The user to move to isolate zone. A normalized form of this parameter will be used for this
	 *             operation.
	 */
	@DELETE
	@Path("{user}/isolate")
	public void isolate(@PathParam("user") final String user) {
		getUserRepository().isolate(securityHelper.getLogin(), checkDeletionRight(user, "isolate"));
	}

	/**
	 * Unlock a user.<br>
	 * Rules, order is important :
	 * <ul>
	 * <li>Only users managing the company of this user can perform the enable, if not, act as if the user did not
	 * exist</li>
	 * <li>User must exist</li>
	 * </ul>
	 * Note : even if the user requesting this enablement has no right on the groups the involved user, this operation can
	 * be performed.
	 *
	 * @param user The user to unlock. A normalized form of this parameter will be used for this operation.
	 */
	@PUT
	@Path("{user}/unlock")
	public void unlock(@PathParam("user") final String user) {
		getUserRepository().unlock(checkDeletionRight(user, "unlock"));
	}

	/**
	 * Restore a user from the isolate zone to the old company.<br>
	 * Rules, order is important :
	 * <ul>
	 * <li>Only users managing the company of this user can perform the enable, if not, act as if the user did not
	 * exist</li>
	 * <li>User must exist</li>
	 * </ul>
	 * Note : even if the user requesting this enablement has no right on the groups the involved user, this operation can
	 * be performed.
	 *
	 * @param user The user to restore. A normalized form of this parameter will be used for this operation.
	 */
	@PUT
	@Path("{user}/restore")
	public void restore(@PathParam("user") final String user) {
		getUserRepository().restore(checkDeletionRight(user, "restore"));
	}

	/**
	 * Reset a user password, send a mail to him and to the user (principal) requesting this action.<br>
	 * This action is audited when succeeded. Rules, order is important :
	 * <ul>
	 * <li>Only users managing the company of this user can perform the operation, if not, act as if the user did not
	 * exist</li>
	 * <li>Target user must exist</li>
	 * <li>Principal user must be an administrator</li>
	 * </ul>
	 * Note: This operation can be performed even if the principal has no right on the groups related to the involved
	 * user.
	 *
	 * @param uid The user identifier to restore. A normalized form of this parameter will be used for this operation.
	 * @return The generated password.
	 */
	@PUT
	@Path("{user}/reset")
	@ResponseBody
	@Produces(MediaType.TEXT_PLAIN)
	public String resetPassword(@PathParam("user") final String uid) {
		if (uid.equals(securityHelper.getLogin())) {
			// Self-service reset password
			final var user = getUserRepository().findByIdExpected(uid);
			return updatePassword(user, false);
		}

		// Administrative operation
		final var user = checkResetRight(uid);
		// Have to generate a new password
		return Optional.ofNullable(updatePassword(user, false)).map(p -> {

			// Unlock account if locked
			getUserRepository().unlock(user);

			// Log the action
			logAdminReset(user);
			return p;
		}).orElse(null);
	}

	/**
	 * Log password reset action triggered by authenticated and privileged user.
	 *
	 * @param user Target user to log.
	 */
	private void logAdminReset(final UserOrg user) {
		final var logReset = new PasswordResetAudit();
		logReset.setLogin(user.getId());
		passwordResetRepository.saveAndFlush(logReset);
	}

	/**
	 * Check the current user can reset the given user password.
	 *
	 * @param user The user to alter.
	 * @return The internal representation of found user.
	 */
	private UserOrg checkResetRight(final String user) {
		// Check the user exists
		final var userOrg = getUserRepository().findByIdExpected(securityHelper.getLogin(), Normalizer.normalize(user));

		// Check the company
		final var companyDn = getCompanyRepository().findById(userOrg.getCompany()).getDn();
		if (delegateRepository.findByMatchingDnForWrite(securityHelper.getLogin(), companyDn, DelegateType.TREE).isEmpty()) {
			// Report this attempt to delete a non-writable user
			log.warn("Attempt to reset the password of a user '{}' out of scope", user);
			throw new ValidationJsonException(USER_KEY, READ_ONLY, "0", "user", "1", user);
		}
		return userOrg;
	}

	/**
	 * Check the current user can delete, enable or disable the given user entry.
	 *
	 * @param user The user to alter.
	 * @param mode Declared action, only used there for the log.
	 * @return The internal representation of found user.
	 */
	private UserOrg checkDeletionRight(final String user, final String mode) {
		// Check the user exists
		final var userOrg = getUserRepository().findByIdExpected(securityHelper.getLogin(), Normalizer.normalize(user));

		// Check the company
		final var companyDn = getCompanyRepository().findById(userOrg.getCompany()).getDn();
		if (delegateRepository.findByMatchingDnForWrite(securityHelper.getLogin(), companyDn, DelegateType.COMPANY).isEmpty()) {
			// Report this attempt to delete a non-writable user
			log.warn("Attempt to {} a user '{}' out of scope", mode, user);
			throw new ValidationJsonException(USER_KEY, READ_ONLY, "0", "user", "1", user);
		}
		return userOrg;
	}

	/**
	 * Check the groups of given users would contain at least another user when it will be deleted.
	 *
	 * @param userOrg   User o delete and to check the memberships.
	 * @param allGroups Map of group by groupName
	 */
	private void checkLastMemberInGroups(final UserOrg userOrg, final Map<String, GroupOrg> allGroups) {
		for (final var group : userOrg.getGroups()) {
			if (allGroups.get(group).getMembers().size() == 1) {
				throw new ValidationJsonException(USER_KEY, "last-member-of-group", "user", userOrg.getId(), GROUP, group);
			}
		}
	}

	/**
	 * Generate a new password of given user and tag it as secured. The password generation is delegated to the first
	 * password plug-in available. When no plug-in is found, the user is not tagged as secured.
	 *
	 * @param user  The user to update.
	 * @param quiet Flag to turn off the possible notification such as mail.
	 * @return The new generated password. When <code>null</code> no password could be generated, and the user is not
	 * tagged as secured.
	 */
	protected String updatePassword(final UserOrg user, final boolean quiet) {
		return applicationContext.getBeansOfType(IPasswordGenerator.class).values().stream().findFirst().map(p -> {
			// Have to generate a new password
			final var password = p.generate(user.getId(), quiet);

			// This user is now secured
			user.setSecured(true);
			return password;
		}).orElse(null);
	}

	/**
	 * Return the {@link UserOrg} list corresponding to the given attribute/value without using cache for the search,
	 * but using it for the instances.
	 *
	 * @param attribute The attribute name to match.
	 * @param value     The attribute value to match.
	 * @return the found users. May be empty.
	 */
	public List<UserOrg> findAllBy(final String attribute, final String value) {
		return getUserRepository().findAllBy(attribute, value);
	}

	/**
	 * Return the {@link UserOrg} corresponding to the given attribute/value without using cache.
	 *
	 * @param user The user to find. A normalized form will be used for the search.
	 * @return the found user or <code>null</code> when not found. Groups are not fetched for this operation.
	 */
	public UserOrg findByIdNoCache(final String user) {
		return getUserRepository().findByIdNoCache(Normalizer.normalize(user));
	}

	/**
	 * Update internal user with the new user. Note the security is not checked there.
	 *
	 * @param userOrg The internal user to update. Note this must be the internal instance
	 * @param newUser The new user data. Note this will not be the stored instance.
	 */
	private void updateCompanyAsNeeded(final UserOrg userOrg, final UserOrg newUser) {
		// Check the company
		if (ObjectUtils.notEqual(userOrg.getCompany(), newUser.getCompany())) {
			// Move the user
			getUserRepository().move(userOrg, getCompanyRepository().findById(newUser.getCompany()));
		}
	}

	/**
	 * Return the group corresponding to the given department.
	 *
	 * @param department The department to match.
	 * @return The group corresponding to the given department or <code>null</code>.
	 */
	private GroupOrg toDepartmentGroup(final String department) {
		return Optional.ofNullable(department).map(getGroupRepository()::findByDepartment).orElse(null);
	}

	/**
	 * Update internal user with the new user for following attributes : department and local identifier. Note the
	 * security is not checked there.
	 *
	 * @param userOrg The user to update. Note this must be the internal instance.
	 * @param newUser The new user data. Note this will not be the stored instance.
	 */
	public void mergeUser(final UserOrg userOrg, final UserOrg newUser) {
		var needUpdate = false;

		// Merge department
		if (ObjectUtils.notEqual(userOrg.getDepartment(), newUser.getDepartment())) {
			// Remove membership from the old department if exist
			Optional.ofNullable(toDepartmentGroup(userOrg.getDepartment())).ifPresent(g -> getGroupRepository().removeUser(userOrg, g.getId()));

			// Add membership to the new department if exist
			Optional.ofNullable(toDepartmentGroup(newUser.getDepartment())).ifPresent(g -> getGroupRepository().addUser(userOrg, g.getId()));

			userOrg.setDepartment(newUser.getDepartment());
			needUpdate = true;
		}

		// Merge local identifier
		if (ObjectUtils.notEqual(userOrg.getLocalId(), newUser.getLocalId())) {
			userOrg.setLocalId(newUser.getLocalId());
		}

		// Updated as needed
		if (needUpdate) {
			getUserRepository().updateUser(userOrg);
		}
	}

	@Override
	public void decorate(final SessionSettings settings) {
		try {
			// Add the user details when available
			settings.getUserSettings().put("userDetails", findById(settings.getUserName()));
		} catch (ValidationJsonException ve) {
			// Ignore this error
			log.debug("User being authenticated is not defined in primary identity provider ");
		}
		// Add user display
		settings.getApplicationSettings().getData().computeIfAbsent("service:id:user-display", configuration::get);
	}
}
