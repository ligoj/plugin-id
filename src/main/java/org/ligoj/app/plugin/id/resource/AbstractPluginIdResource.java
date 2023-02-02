/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.api.Normalizer;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamConfigurationProvider;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.resource.plugin.AbstractToolPluginResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;
import jakarta.ws.rs.NotAuthorizedException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Plug-in Identity base class.
 *
 * @param <U> The {@link IUserRepository} type.
 */
@Slf4j
public abstract class AbstractPluginIdResource<U extends IUserRepository> extends AbstractToolPluginResource
		implements IdentityServicePlugin, IamConfigurationProvider {

	/**
	 * Lock object used to synchronize the creation.
	 */
	private static final Object USER_LOCK = new Object();

	/**
	 * The default authentication property name.
	 */
	private static final String DEFAULT_ID = "id";

	/**
	 * Available node configurations. Key is the node identifier.
	 */
	private final Map<String, IamConfiguration> nodeConfigurations = new HashMap<>();

	@Autowired
	protected UserOrgResource userResource;

	protected abstract AbstractPluginIdResource<U> getSelf();

	@Override
	public Authentication authenticate(final Authentication authentication, final String node, final boolean primary) {
		@SuppressWarnings("unchecked")
		final var repository = (U) getSelf().getConfiguration(node).getUserRepository();

		// Authenticate the user
		if (repository.authenticate(authentication.getName(), (String) authentication.getCredentials())) {
			// Return a new authentication based on resolved application user
			return primary ? authentication
					: new UsernamePasswordAuthenticationToken(toApplicationUser(repository, authentication), null);
		}
		throw new BadCredentialsException("");
	}

	/**
	 * Check the authentication, then create or get the application user matching to the given account.
	 *
	 * @param repository     Repository used to authenticate the user, and also to use to fetch the user attributes.
	 * @param authentication The current authentication.
	 * @return A not <code>null</code> application user.
	 */
	protected String toApplicationUser(final U repository, final Authentication authentication) {
		// Check the authentication and get the user from its repository
		final var account = repository.findOneBy(getAuthenticateProperty(repository, authentication),
				authentication.getName());

		// Check at least one mail is present for the federation
		if (CollectionUtils.isEmpty(account.getMails())) {
			// Mails are required to proceed the federation between the repositories
			log.info("Account '{} [{} {}]' has no mail", account.getId(), account.getFirstName(),
					account.getLastName());
			throw new NotAuthorizedException("ambiguous-account-no-mail");
		}

		// Find (or create) the corresponding application user
		return toApplicationUser(account);
	}

	/**
	 * Return the local identity property name. In the most case, will be a constant, but in some cases, the property
	 * name is determined dynamically depending on the available authentication principal
	 * 
	 * @param repository     The current repository.
	 * @param authentication The current authentication.
	 * @return The property name.
	 */
	protected String getAuthenticateProperty(final U repository, final Authentication authentication) {
		return DEFAULT_ID;
	}

	/**
	 * Create or get the application user matching to the given account.
	 *
	 * @param account The account from the authentication.
	 * @return A not <code>null</code> application user.
	 */
	protected String toApplicationUser(final UserOrg account) {
		// Find the user by the mail in the primary repository
		final var usersByMail = userResource.findAllBy("mails", account.getMails().get(0));
		if (usersByMail.isEmpty()) {
			// No more try, account can be created in the application repository with a free login
			return newApplicationUser(account);
		}
		if (usersByMail.size() == 1) {
			// Everything is checked, account can be merged into the existing application user
			userResource.mergeUser(usersByMail.get(0), account);
			return usersByMail.get(0).getId();
		}

		// Too many matching mail
		log.info("Account '{} [{} {}]' has too many mails ({}), expected one", account.getId(), account.getFirstName(),
				account.getLastName(), usersByMail.size());
		throw new NotAuthorizedException("ambiguous-account-too-many-mails");
	}

	/**
	 * Create the application user from the actual account.
	 *
	 * @param account The account from the authentication.
	 * @return The new application user.
	 */
	public String newApplicationUser(final UserOrg account) {
		synchronized (USER_LOCK) {

			// Copy the data from the authenticated account to the application account
			final var userEdition = new UserOrgEditionVo();
			account.copy(userEdition);
			userEdition.setGroups(Collections.emptyList());
			userEdition.setMail(account.getMails().get(0));

			// Assign a free login
			userEdition.setName(nextFreeLogin(toLogin(account)));

			// This user can be created in the primary repository
			userResource.saveOrUpdate(userEdition);

			return userEdition.getId();
		}
	}

	/**
	 * Find a free application login from a base login. Primary repository is checked to reclaim a free login.
	 *
	 * @param login The base login name.
	 * @return a free login inside the primary repository.
	 */
	protected String nextFreeLogin(final String login) {
		var suffix = 0;
		UserOrg user;
		String nextLogin;
		do {
			nextLogin = login + (suffix == 0 ? "" : suffix);
			user = userResource.findByIdNoCache(nextLogin);
			suffix++;
		} while (user != null);

		// No user found for this login
		return nextLogin;
	}

	/**
	 * Generate an application login from an account.
	 *
	 * @param account The current authenticated account in this security provider.
	 * @return a corresponding application login candidate from an account.
	 */
	protected String toLogin(final UserOrg account) {
		final var trimFirstName = normalize(account.getFirstName());
		final var trimLastName = normalize(account.getLastName());
		if (trimFirstName.length() * trimLastName.length() == 0) {
			// Unable to build a valid login from these attributes
			throw new NotAuthorizedException("cannot-build-application-login");
		}

		return trimFirstName.charAt(0) + trimLastName;
	}

	private String normalize(final String string) {
		return StringUtils.trimToEmpty(Normalizer.normalize(string).replace("[^\\w\\d]", " ").replace("  ", " "));
	}

	@Override
	public IamConfiguration getConfiguration(final String node) {
		getSelf().ensureCachedConfiguration(node);
		return nodeConfigurations.computeIfAbsent(node, this::refreshConfiguration);
	}

	/**
	 * Ensure the configuration is loaded for the given node. Cache is involved.
	 * 
	 * @param node The node identifier, also used as cache key.
	 * @return The IAM configuration related to the given node.
	 */
	@CacheResult(cacheName = "id-configuration")
	public boolean ensureCachedConfiguration(@CacheKey final String node) {
		refreshConfiguration(node);
		return true;
	}

	/**
	 * Build a user SQL repository from the given node.
	 *
	 * @param node The node identifier, also used as cache key.
	 * @return The {@link IUserRepository} instance. Cache is involved.
	 */
	protected abstract U getUserRepository(final String node);

	/**
	 * Refresh the IAM configuration related to the given node. The {@link #nodeConfigurations} is replaced by a new
	 * {@link IamConfiguration} instance.
	 * 
	 * @param node The node identifier.
	 * @return The IAM configuration related to the given node.
	 */
	protected IamConfiguration refreshConfiguration(final String node) {
		return nodeConfigurations.compute(node, (n, m) -> {
			final var iam = new IamConfiguration();
			final var repository = getUserRepository(node);
			iam.setNode(node);
			iam.setUserRepository(repository);
			copyConfiguration(iam, repository);
			return iam;
		});
	}

	/**
	 * Copy the repository details to the IAM configuration.
	 * 
	 * @param iam        The target IAM configuration.
	 * @param repository The current {@link IUserRepository} instance.
	 */
	protected void copyConfiguration(final IamConfiguration iam, final U repository) {
		iam.setCompanyRepository(repository.getCompanyRepository());
		iam.setGroupRepository(repository.getGroupRepository());
	}
}
