package org.ligoj.app.plugin.id.resource.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.DefaultVerificationMode;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.plugin.id.resource.UserOrgEditionVo;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test of {@link UserBatchUpdateResource}
 */
public class UserBatchUpdateResourceTest extends AbstractUserBatchResourceTest {

	@Autowired
	protected UserBatchUpdateResource resource;

	@Test
	public void executeCsvError() {
		final UserBatchUpdateResource resource = new UserBatchUpdateResource();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		Assertions.assertEquals(
				"csv-file:Too much values for type org.ligoj.app.plugin.id.resource.batch.UserUpdateEntry. Expected : 1, got : 2 : [fdaugan, mail]",
				Assertions.assertThrows(ValidationJsonException.class,
						() -> resource.batch(new ByteArrayInputStream("fdaugan;mail".getBytes("cp1252")),
								new String[] { "user" }, "cp1252", new String[] { "user" }, UserUpdateEntry.class,
								UserAtomicTask.class, false))
						.getMessage());
	}

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
		Mockito.when(mockResource.findById("fdaugan")).thenReturn(user);

		final long id = resource.execute(
				new ByteArrayInputStream("fdaugan;mail;any.daugan@sample.com".getBytes("cp1252")),
				new String[] { "user", "operation", "value" }, "cp1252", false);
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

		// Check user
		Mockito.verify(mockResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 2) {
				throw new MockitoException("Expect two calls");
			}

			// "findBy" call
			Assertions.assertEquals("fdaugan", data.getAllInvocations().get(0).getArguments()[0]);

			// "update" call
			final UserOrgEditionVo userEdit = (UserOrgEditionVo) data.getAllInvocations().get(1).getArguments()[0];
			Assertions.assertNotNull(userEdit);
			Assertions.assertEquals("untouched", userEdit.getFirstName());
			Assertions.assertEquals("untouched", userEdit.getLastName());
			Assertions.assertEquals("fdaugan", userEdit.getId());
			Assertions.assertEquals("untouched", userEdit.getCompany());
			Assertions.assertEquals("untouched", userEdit.getDepartment());
			Assertions.assertEquals("untouched", userEdit.getLocalId());
			Assertions.assertEquals("any.daugan@sample.com", userEdit.getMail());
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
