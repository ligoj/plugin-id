package org.ligoj.app.plugin.id.resource.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.DefaultVerificationMode;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.plugin.id.resource.UserOrgEditionVo;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test of {@link UserBatchLdapResource}
 */
public class UserBatchUpdateLdapResourceTest extends AbstractUserBatchLdapResourceTest {

	@Autowired
	protected UserBatchUpdateLdapResource resource;

	@Test
	public void execute() throws IOException, InterruptedException {
		initSpringSecurityContext(DEFAULT_USER);

		final UserOrg user = new UserOrg();
		user.setCompany("untouched");
		user.setDepartment("untouched");
		user.setFirstName("untouched");
		user.setLastName("untouched");
		user.setName("fdaugan");
		user.setLocalId("untouched");
		user.setMails(new ArrayList<>());
		user.setGroups(new ArrayList<>());
		Mockito.when(mockLdapResource.findById("fdaugan")).thenReturn(user);

		final long id = resource.execute(
				new ByteArrayInputStream("fdaugan;mail;any.daugan@sample.com".getBytes("cp1252")),
				new String[] { "user", "operation", "value" }, "cp1252");
		Assertions.assertNotNull(id);
		BatchTaskVo<UserUpdateEntry> importTask = resource.getImportTask(id);
		Assertions.assertEquals(id, importTask.getId());
		importTask = waitImport(importTask);

		// Check the result
		final UserUpdateEntry importEntry = checkImportTask(importTask);
		Assertions.assertEquals("mail", importEntry.getOperation());
		Assertions.assertEquals("any.daugan@sample.com", importEntry.getValue());
		Assertions.assertEquals("fdaugan", importEntry.getUser());
		Assertions.assertTrue(importEntry.getStatus());
		Assertions.assertNull(importEntry.getStatusText());

		// Check LDAP
		Mockito.verify(mockLdapResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 2) {
				throw new MockitoException("Expect two calls");
			}

			// "findBy" call
			Assertions.assertEquals("fdaugan", data.getAllInvocations().get(0).getArguments()[0]);

			// "update" call
			final UserOrgEditionVo userLdap = (UserOrgEditionVo) data.getAllInvocations().get(1).getArguments()[0];
			Assertions.assertNotNull(userLdap);
			Assertions.assertEquals("untouched", userLdap.getFirstName());
			Assertions.assertEquals("untouched", userLdap.getLastName());
			Assertions.assertEquals("fdaugan", userLdap.getId());
			Assertions.assertEquals("untouched", userLdap.getCompany());
			Assertions.assertEquals("untouched", userLdap.getDepartment());
			Assertions.assertEquals("untouched", userLdap.getLocalId());
			Assertions.assertEquals("any.daugan@sample.com", userLdap.getMail());
		})).update(null);
	}

	@Test
	public void getImportTaskFailed() {
		Assertions.assertNull(resource.getImportTask(-1));
	}

	@Test
	public void getImportStatusFailed() {
		Assertions.assertNull(resource.getImportStatus(-1));
	}

}
