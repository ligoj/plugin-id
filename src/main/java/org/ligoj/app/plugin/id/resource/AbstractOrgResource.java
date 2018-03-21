/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamProvider;
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

	/**
	 * User repository provider.
	 * 
	 * @return User repository provider.
	 */
	protected IUserRepository getUser() {
		return iamProvider[0].getConfiguration().getUserRepository();
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
	 * Group repository provider.
	 * 
	 * @return Group repository provider.
	 */
	protected IGroupRepository getGroup() {
		return iamProvider[0].getConfiguration().getGroupRepository();
	}

}
