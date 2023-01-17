/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.validation.ConstraintViolationException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.DefaultVerificationMode;
import org.ligoj.app.plugin.id.resource.UserOrgEditionVo;
import org.ligoj.bootstrap.MatcherUtil;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Test of {@link UserBatchImportResource}
 */
class UserBatchImportResourceTest extends AbstractUserBatchResourceTest {

	@Autowired
	protected UserBatchImportResource resource;

	@Test
	void execute() throws IOException, InterruptedException {
		final BatchTaskVo<UserImportEntry> importTask = execute(
				"Loubli;Sébastien;kloubli;my.address@sample.com;ligoj;jira;MY_DPT;MY_ID");

		// Check the result
		final UserImportEntry importEntry = checkImportTask(importTask);
		Assertions.assertEquals("ligoj", importEntry.getCompany());
		Assertions.assertEquals("Sébastien", importEntry.getFirstName());
		Assertions.assertEquals("Loubli", importEntry.getLastName());
		Assertions.assertEquals("kloubli", importEntry.getId());
		Assertions.assertEquals("jira", importEntry.getGroups());
		Assertions.assertEquals("my.address@sample.com", importEntry.getMail());
		Assertions.assertTrue(importEntry.getStatus());
		Assertions.assertNull(importEntry.getStatusText());

		// Check user
		Mockito.verify(mockResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 1) {
				throw new MockitoException("Expect one call");
			}
			final UserOrgEditionVo user = (UserOrgEditionVo) data.getAllInvocations().get(0).getArguments()[0];
			Assertions.assertNotNull(user);
			Assertions.assertEquals("Sébastien", user.getFirstName());
			Assertions.assertEquals("Loubli", user.getLastName());
			Assertions.assertEquals("kloubli", user.getId());
			Assertions.assertEquals("ligoj", user.getCompany());
			Assertions.assertEquals("my.address@sample.com", user.getMail());
			Assertions.assertEquals("MY_DPT", user.getDepartment());
			Assertions.assertEquals("MY_ID", user.getLocalId());
		})).create(null);
	}

	protected <U extends BatchElement> BatchTaskVo<U> execute(final InputStream input, final String[] headers)
			throws IOException, InterruptedException {
		return execute(input, headers, "cp1252");
	}

	protected <U extends BatchElement> BatchTaskVo<U> execute(final InputStream input, final String[] headers,
			final String encoding) throws IOException, InterruptedException {
		initSpringSecurityContext(DEFAULT_USER);
		final long id = resource.execute(input, headers, encoding, false);
		@SuppressWarnings("unchecked")
		final BatchTaskVo<U> importTask = (BatchTaskVo<U>) resource.getImportTask(id);
		Assertions.assertEquals(id, importTask.getId());
		return waitImport(importTask);
	}

	protected <U extends BatchElement> BatchTaskVo<U> execute(final String csvData)
			throws IOException, InterruptedException {
		return execute(csvData, "cp1252");
	}

	protected <U extends BatchElement> BatchTaskVo<U> execute(final String csvData, final String encoding)
			throws IOException, InterruptedException {
		return execute(new ByteArrayInputStream(csvData.getBytes(encoding)),
				new String[] { "lastName", "firstName", "id", "mail", "company", "groups", "department", "localId" },
				encoding);
	}

	@Test
	void executeDefaultHeader() throws IOException, InterruptedException {
		final InputStream input = new ByteArrayInputStream(
				"Loubli;Sébastien;kloubli5;my.address@sample.com;ligoj;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		final BatchTaskVo<UserImportEntry> importTask = waitImport(
				resource.getImportTask(resource.execute(input, new String[0], "cp1250", false)));
		Assertions.assertEquals(Boolean.TRUE, importTask.getEntries().get(0).getStatus());
		Assertions.assertNull(importTask.getEntries().get(0).getStatusText());

		// Check user
		Mockito.verify(mockResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 1) {
				throw new MockitoException("Expect one call");
			}
			final UserOrgEditionVo user = (UserOrgEditionVo) data.getAllInvocations().get(0).getArguments()[0];
			Assertions.assertNotNull(user);
			Assertions.assertNotNull(user);
			Assertions.assertEquals("Sébastien", user.getFirstName());
			Assertions.assertEquals("Loubli", user.getLastName());
			Assertions.assertEquals("kloubli5", user.getId());
			Assertions.assertEquals("ligoj", user.getCompany());
			Assertions.assertEquals("my.address@sample.com", user.getMail());
		})).create(null);
	}

	@Test
	void executeEmptyGroups() throws IOException, InterruptedException {
		final BatchTaskVo<UserImportEntry> importTask = execute(
				"Loubli;Sébastien;kloubli9;my.address@sample.com;ligoj;,jira,");

		// Check the result
		final UserImportEntry importEntry = checkImportTask(importTask);
		Assertions.assertEquals("kloubli9", importEntry.getId());
		Assertions.assertTrue(importEntry.getStatus());
		Assertions.assertNull(importEntry.getStatusText());

		// Check user
		Mockito.verify(mockResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 1) {
				throw new MockitoException("Expect one call");
			}
			final UserOrgEditionVo user = (UserOrgEditionVo) data.getAllInvocations().get(0).getArguments()[0];
			Assertions.assertNotNull(user);
			Assertions.assertEquals("kloubli9", user.getId());
			Assertions.assertEquals(1, user.getGroups().size());
			Assertions.assertEquals("jira", user.getGroups().iterator().next());
		})).create(null);
	}

	@Test
	void executeFailed() throws IOException, InterruptedException {
		Mockito.doThrow(new BusinessException("message")).when(this.mockResource)
				.create(ArgumentMatchers.any(UserOrgEditionVo.class), ArgumentMatchers.eq(false));
		final InputStream input = new ByteArrayInputStream(
				"Loubli;Sébastien;fdaugan;my.address@sample.com;ligoj;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		final BatchTaskVo<UserImportEntry> importTask = waitImport(
				resource.getImportTask(resource.execute(input, new String[0], "cp1250", false)));
		Assertions.assertEquals("message", importTask.getEntries().get(0).getStatusText());
		Assertions.assertEquals(Boolean.FALSE, importTask.getEntries().get(0).getStatus());
	}

	@Test
	void executeInvalidHeaders() throws IOException {
		final InputStream input = new ByteArrayInputStream(
				"Loubli;Sébastien;kloubli4;my.address@sample.com;ligoj;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		Assertions.assertEquals("csv-file:Invalid header __?__", Assertions.assertThrows(ValidationJsonException.class, () -> {
			resource.execute(input, new String[] { "lastName", "firstName", "id", "__?__", "company", "groups" },
					"cp1250", false);
		}).getMessage());
	}

	@Test
	void executeMisingLogin() throws IOException {
		final InputStream input = new ByteArrayInputStream(
				"Loubli;Sébastien;;my.address@sample.com;ligoj;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		MatcherUtil.assertThrows(Assertions.assertThrows(ConstraintViolationException.class, () -> {
			resource.execute(input, new String[0], "cp1250", false);
		}), "id", "NotBlank");
	}

	@Test
	void getImportStatus() throws InterruptedException, IOException {
		final BatchTaskVo<UserImportEntry> importTask = execute(
				"Loubli;Sébastien;kloubli7;my.address@sample.com;ligoj;,jira,");
		Assertions.assertSame(importTask, resource.getImportTask(importTask.getId()));
		Assertions.assertSame(importTask.getStatus(), resource.getImportStatus(importTask.getId()));
	}

	@Test
	void getImportStatusFailed() {
		Assertions.assertNull(resource.getImportStatus(-1));
	}

	@Test
	void getImportStatusPreviousFinished() throws InterruptedException, IOException {
		final BatchTaskVo<UserImportEntry> oldTask = execute(
				"Loubli;Sébastien;kloubli5a;my.address@sample.com;ligoj;,jira,");
		oldTask.getStatus().setEnd(getDate(1980, 1, 1));
		final BatchTaskVo<UserImportEntry> importTask = execute(
				"Loubli;Sébastien;kloubli5b;my.address@sample.com;ligoj;,jira,");
		Assertions.assertSame(importTask, resource.getImportTask(importTask.getId()));
		Assertions.assertSame(importTask.getStatus(), resource.getImportStatus(importTask.getId()));
	}

	@Test
	void getImportStatusPreviousNotFinished() throws InterruptedException, IOException {
		final BatchTaskVo<UserImportEntry> oldTask = execute(
				"Loubli;Sébastien;kloubli6a;my.address@sample.com;ligoj;,jira,");
		oldTask.getStatus().setEnd(null);
		final BatchTaskVo<UserImportEntry> importTask = execute(
				"Loubli;Sébastien;kloubli6b;my.address@sample.com;ligoj;,jira,");
		Assertions.assertSame(importTask, resource.getImportTask(importTask.getId()));
		Assertions.assertSame(importTask.getStatus(), resource.getImportStatus(importTask.getId()));
		oldTask.getStatus().setEnd(new Date());
	}

	@Test
	void getImportTaskFailed() {
		Assertions.assertNull(resource.getImportTask(-1));
	}

}
