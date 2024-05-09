/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Setter;

/**
 * Base class for organizational resource management.
 */
public abstract class AbstractOrgResource {

	/**
	 * IAM provider.
	 */
	@Autowired
	@Setter
	protected IamProvider[] iamProvider;

	@Autowired
	protected IdentityResource resource;

	@Autowired
	protected SecurityHelper securityHelper;

	/**
	 * User repository provider.
	 *
	 * @return User repository provider.
	 */
	public IUserRepository getUserRepository() {
		return iamProvider[0].getConfiguration().getUserRepository();
	}

	/**
	 * Company repository provider.
	 *
	 * @return Company repository provider.
	 */
	public ICompanyRepository getCompanyRepository() {
		return iamProvider[0].getConfiguration().getCompanyRepository();
	}

	/**
	 * Group repository provider.
	 *
	 * @return Group repository provider.
	 */
	public IGroupRepository getGroupRepository() {
		return iamProvider[0].getConfiguration().getGroupRepository();
	}
}
