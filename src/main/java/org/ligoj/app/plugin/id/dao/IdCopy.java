/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.UnaryOperator;

import jakarta.transaction.Transactional;

import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.ResourceOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.dao.CacheCompanyRepository;
import org.ligoj.app.iam.dao.CacheGroupRepository;
import org.ligoj.app.iam.dao.CacheMembershipRepository;
import org.ligoj.app.iam.dao.CacheUserRepository;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.plugin.id.dao.AbstractMemCacheRepository.CacheDataType;
import org.ligoj.app.plugin.id.model.CacheCopyState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

/**
 * The database copy of the identity data: the shared cache tables, with all the user attributes a provider reads, and
 * the {@link CacheCopyState} of the node that wrote them. After a restart, a provider rebuilds its memory from it,
 * without reading its server, as long as it is fresh and comes from its current configuration.
 */
@Repository
@Slf4j
@Transactional
public class IdCopy {

	/**
	 * The copy rebuilt in memory, and the time of the full read it comes from.
	 *
	 * @param data         The groups, companies and users, as a full read builds them.
	 * @param lastFullRead The time of that full read.
	 */
	public record Snapshot(Map<CacheDataType, Map<String, ? extends ResourceOrg>> data, Instant lastFullRead) {
	}

	private static final ObjectMapper MAPPER = new ObjectMapper();

	private static final TypeReference<List<String>> MAILS = new TypeReference<>() {
		// Nothing to override
	};

	private static final TypeReference<Map<String, String>> ATTRIBUTES = new TypeReference<>() {
		// Nothing to override
	};

	@Autowired
	private CacheCopyStateRepository stateRepository;

	@Autowired
	private CacheUserRepository cacheUserRepository;

	@Autowired
	private CacheGroupRepository cacheGroupRepository;

	@Autowired
	private CacheCompanyRepository cacheCompanyRepository;

	@Autowired
	private CacheMembershipRepository cacheMembershipRepository;

	/**
	 * Return the JSON text of a value, <code>null</code> for <code>null</code>.
	 *
	 * @param value The value: a list of mails, a map of custom attributes.
	 * @return The JSON text.
	 */
	static String toJson(final Object value) {
		return value == null ? null : MAPPER.writeValueAsString(value);
	}

	/**
	 * Save the state of the copy after a complete full read: the shared tables hold the data of this node only, so the
	 * state of any other node is removed.
	 *
	 * @param node         The node identifier.
	 * @param fingerprint  The fingerprint of the configuration the data was read with.
	 * @param lastFullRead The time of the full read.
	 */
	@Transactional(Transactional.TxType.REQUIRES_NEW)
	public void saveState(final String node, final String fingerprint, final Instant lastFullRead) {
		stateRepository.deleteAll(stateRepository.findAll().stream().filter(s -> !s.getId().equals(node)).toList());
		final var state = stateRepository.findById(node).orElseGet(CacheCopyState::new);
		state.setId(node);
		state.setFingerprint(fingerprint);
		state.setLastFullRead(lastFullRead);
		stateRepository.saveAndFlush(state);
	}

	/**
	 * Rebuild the data of the node from the copy.
	 *
	 * @param node         The node identifier.
	 * @param fingerprint  The fingerprint of the current configuration of the node.
	 * @param now          The current time.
	 * @param nextFullRead When the full read following a given one is due: the copy expires then.
	 * @param companies    The companies of the provider, from their rows.
	 * @return The rebuilt data, or empty when the copy cannot be trusted: no copy of this node, another configuration,
	 *         or too old.
	 */
	public Optional<Snapshot> load(final String node, final String fingerprint, final Instant now,
			final UnaryOperator<Instant> nextFullRead, final Function<Collection<CacheCompany>, Map<String, CompanyOrg>> companies) {
		final var state = stateRepository.findById(node).orElse(null);
		if (state == null) {
			return refuse("no copy");
		}
		if (!fingerprint.equals(state.getFingerprint())) {
			return refuse("configuration changed");
		}
		if (!now.isBefore(nextFullRead.apply(state.getLastFullRead()))) {
			return refuse("expired");
		}
		final Map<CacheDataType, Map<String, ? extends ResourceOrg>> data = new EnumMap<>(CacheDataType.class);
		final var groups = toGroups(cacheGroupRepository.findAll(), cacheMembershipRepository.findAll());
		data.put(CacheDataType.COMPANY, companies.apply(cacheCompanyRepository.findAll()));
		data.put(CacheDataType.GROUP, groups);
		data.put(CacheDataType.USER, toUsers(cacheUserRepository.findAll(), groups));
		return Optional.of(new Snapshot(data, state.getLastFullRead()));
	}

	private Optional<Snapshot> refuse(final String reason) {
		log.info("Full read required: {}", reason);
		return Optional.empty();
	}

	/**
	 * Rebuild the groups as a full read builds them: user members by identifier, sub-groups and parents.
	 */
	private static Map<String, GroupOrg> toGroups(final Collection<CacheGroup> rows, final Collection<CacheMembership> memberships) {
		final var groups = new HashMap<String, GroupOrg>();
		for (final var row : rows) {
			final var group = new GroupOrg(row.getDescription(), row.getName(), new HashSet<>());
			groups.put(group.getId(), group);
		}
		for (final var membership : memberships) {
			final var group = groups.get(membership.getGroup().getId());
			if (membership.getUser() == null) {
				// Sub-group membership, both sides
				group.getSubGroups().add(membership.getSubGroup().getId());
				groups.get(membership.getSubGroup().getId()).setParent(group.getId());
			} else {
				group.getMembers().add(membership.getUser().getId());
			}
		}
		return groups;
	}

	/**
	 * Rebuild the users as a full read builds them, the groups from the members of the rebuilt groups.
	 */
	private static Map<String, UserOrg> toUsers(final Collection<CacheUser> rows, final Map<String, GroupOrg> groups) {
		final var users = new HashMap<String, UserOrg>();
		for (final var row : rows) {
			final var user = new UserOrg();
			user.setId(row.getId());
			user.setFirstName(row.getFirstName());
			user.setLastName(row.getLastName());
			user.setCompany(Optional.ofNullable(row.getCompany()).map(CacheCompany::getId).orElse(null));
			user.setDn(row.getDn());
			user.setMails(new ArrayList<>(read(row.getMailList(), MAILS, List.of())));
			user.setSecured(Boolean.TRUE.equals(row.getSecured()));
			user.setLocked(row.getLocked());
			user.setLockedBy(row.getLockedBy());
			user.setIsolated(row.getIsolated());
			user.setDepartment(row.getDepartment());
			user.setLocalId(row.getLocalId());
			user.setCustomAttributes(new HashMap<>(read(row.getCustomAttributes(), ATTRIBUTES, Map.of())));
			user.setGroups(new ArrayList<>());
			users.put(user.getId(), user);
		}
		groups.values().forEach(g -> g.getMembers().forEach(u -> users.get(u).getGroups().add(g.getId())));
		return users;
	}

	private static <T> T read(final String json, final TypeReference<T> type, final T empty) {
		return json == null ? empty : MAPPER.readValue(json, type);
	}
}
