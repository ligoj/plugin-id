package org.ligoj.app.plugin.id.resource.batch;

import org.ligoj.app.plugin.id.resource.UserOrgEditionVo;
import org.ligoj.app.plugin.id.resource.UserOrgResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * Import from list of bean entries.
 */
@Component
@Scope(value = ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class UserFullTask extends AbstractBatchTask<UserImportEntry> {

	@Autowired
	protected UserOrgResource resource;

	@Override
	protected void doBatch(final UserImportEntry entry, final boolean quiet) {

		// Copy the user information
		final UserOrgEditionVo user = new UserOrgEditionVo();
		user.setCompany(entry.getCompany());
		user.setFirstName(entry.getFirstName());
		user.setLastName(entry.getLastName());
		user.setId(entry.getId());
		user.setMail(entry.getMail());
		user.setDepartment(entry.getDepartment());
		user.setLocalId(entry.getLocalId());

		// Copy groups
		user.setGroups(toList(entry.getGroups()));

		// Create the user
		resource.create(user, quiet);
	}

}