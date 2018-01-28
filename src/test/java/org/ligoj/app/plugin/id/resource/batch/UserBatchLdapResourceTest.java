package org.ligoj.app.plugin.id.resource.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;

import javax.transaction.Transactional;
import javax.validation.ConstraintViolationException;

import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.DefaultVerificationMode;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.plugin.id.resource.UserOrgEditionVo;
import org.ligoj.app.plugin.id.resource.UserOrgResource;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test of {@link UserBatchLdapResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class UserBatchLdapResourceTest extends AbstractLdapBatchTest {

	@Autowired
	protected UserBatchLdapResource resource;

	private UserOrgResource mockLdapResource;

	@SuppressWarnings("unchecked")
	@BeforeEach
	public void mockApplicationContext() {
		final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
		SpringUtils.setSharedApplicationContext(applicationContext);
		mockLdapResource = Mockito.mock(UserOrgResource.class);
		final UserFullLdapTask mockTask = new UserFullLdapTask();
		mockTask.resource = mockLdapResource;
		mockTask.securityHelper = securityHelper;
		final UserAtomicLdapTask mockTaskUpdate = new UserAtomicLdapTask();
		mockTaskUpdate.resource = mockLdapResource;
		mockTaskUpdate.securityHelper = securityHelper;
		Mockito.when(applicationContext.getBean(SessionSettings.class)).thenReturn(new SessionSettings());
		Mockito.when(applicationContext.getBean((Class<?>) ArgumentMatchers.any(Class.class))).thenAnswer((Answer<Object>) invocation -> {
			final Class<?> requiredType = (Class<Object>) invocation.getArguments()[0];
			if (requiredType == UserFullLdapTask.class) {
				return mockTask;
			}
			if (requiredType == UserAtomicLdapTask.class) {
				return mockTaskUpdate;
			}
			return UserBatchLdapResourceTest.super.applicationContext.getBean(requiredType);
		});

		mockTaskUpdate.jaxrsFactory = ServerProviderFactory.createInstance(null);
	}

	@AfterEach
	public void unmockApplicationContext() {
		SpringUtils.setSharedApplicationContext(super.applicationContext);
	}

	@BeforeEach
	public void prepareData() throws IOException {
		persistEntities("csv", new Class[] { DelegateOrg.class }, StandardCharsets.UTF_8.name());
	}

	@Test
	public void full() throws IOException, InterruptedException {
		final BatchTaskVo<UserImportEntry> importTask = full("Loubli;Sébastien;kloubli;my.address@sample.com;gfi;jira");

		// Check the result
		final UserImportEntry importEntry = checkImportTask(importTask);
		Assertions.assertEquals("gfi", importEntry.getCompany());
		Assertions.assertEquals("Sébastien", importEntry.getFirstName());
		Assertions.assertEquals("Loubli", importEntry.getLastName());
		Assertions.assertEquals("kloubli", importEntry.getId());
		Assertions.assertEquals("jira", importEntry.getGroups());
		Assertions.assertEquals("my.address@sample.com", importEntry.getMail());
		Assertions.assertTrue(importEntry.getStatus());
		Assertions.assertNull(importEntry.getStatusText());

		// Check LDAP
		Mockito.verify(mockLdapResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 1) {
				throw new MockitoException("Expect one call");
			}
			final UserOrgEditionVo userLdap = (UserOrgEditionVo) data.getAllInvocations().get(0).getArguments()[0];
			Assertions.assertNotNull(userLdap);
			Assertions.assertEquals("Sébastien", userLdap.getFirstName());
			Assertions.assertEquals("Loubli", userLdap.getLastName());
			Assertions.assertEquals("kloubli", userLdap.getId());
			Assertions.assertEquals("gfi", userLdap.getCompany());
			Assertions.assertEquals("my.address@sample.com", userLdap.getMail());
		})).create(null);
	}

	@Test
	public void atomic() throws IOException, InterruptedException {
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

		final long id = resource.atomic(new ByteArrayInputStream("fdaugan;mail;any.daugan@sample.com".getBytes("cp1252")),
				new String[] { "user", "operation", "value" }, "cp1252");
		Assertions.assertNotNull(id);
		@SuppressWarnings("unchecked")
		BatchTaskVo<UserUpdateEntry> importTask = (BatchTaskVo<UserUpdateEntry>) resource.getImportTask(id);
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
	public void fullEmptyGroups() throws IOException, InterruptedException {
		final BatchTaskVo<UserImportEntry> importTask = full("Loubli;Sébastien;kloubli9;my.address@sample.com;gfi;,jira,");

		// Check the result
		final UserImportEntry importEntry = checkImportTask(importTask);
		Assertions.assertEquals("kloubli9", importEntry.getId());
		Assertions.assertTrue(importEntry.getStatus());
		Assertions.assertNull(importEntry.getStatusText());

		// Check LDAP
		Mockito.verify(mockLdapResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 1) {
				throw new MockitoException("Expect one call");
			}
			final UserOrgEditionVo userLdap = (UserOrgEditionVo) data.getAllInvocations().get(0).getArguments()[0];
			Assertions.assertNotNull(userLdap);
			Assertions.assertEquals("kloubli9", userLdap.getId());
			Assertions.assertEquals(1, userLdap.getGroups().size());
			Assertions.assertEquals("jira", userLdap.getGroups().iterator().next());
		})).create(null);
	}

	@Test
	public void fullInvalidHeaders() throws IOException {
		final InputStream input = new ByteArrayInputStream("Loubli;Sébastien;kloubli4;my.address@sample.com;gfi;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		Assertions.assertEquals("Invalid header", Assertions.assertThrows(BusinessException.class, () -> {
			resource.full(input, new String[] { "lastName", "firstName", "id", "mail8", "company", "groups" }, "cp1250");
		}).getMessage());
	}

	@Test
	public void fullDefaultHeader() throws IOException, InterruptedException {
		final InputStream input = new ByteArrayInputStream("Loubli;Sébastien;kloubli5;my.address@sample.com;gfi;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		@SuppressWarnings("unchecked")
		final BatchTaskVo<UserImportEntry> importTask = (BatchTaskVo<UserImportEntry>) waitImport(
				resource.getImportTask(resource.full(input, new String[0], "cp1250")));
		Assertions.assertEquals(Boolean.TRUE, importTask.getEntries().get(0).getStatus());
		Assertions.assertNull(importTask.getEntries().get(0).getStatusText());

		// Check LDAP
		Mockito.verify(mockLdapResource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 1) {
				throw new MockitoException("Expect one call");
			}
			final UserOrgEditionVo userLdap = (UserOrgEditionVo) data.getAllInvocations().get(0).getArguments()[0];
			Assertions.assertNotNull(userLdap);
			Assertions.assertNotNull(userLdap);
			Assertions.assertEquals("Sébastien", userLdap.getFirstName());
			Assertions.assertEquals("Loubli", userLdap.getLastName());
			Assertions.assertEquals("kloubli5", userLdap.getId());
			Assertions.assertEquals("gfi", userLdap.getCompany());
			Assertions.assertEquals("my.address@sample.com", userLdap.getMail());
		})).create(null);
	}

	@Test
	public void fullMisingLogin() throws IOException {
		final InputStream input = new ByteArrayInputStream("Loubli;Sébastien;;my.address@sample.com;gfi;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		MatcherUtil.assertThrows(Assertions.assertThrows(ConstraintViolationException.class, () -> {
			resource.full(input, new String[0], "cp1250");
		}), "id", "NotBlank");
	}

	@Test
	public void fullFailed() throws IOException, InterruptedException {
		Mockito.doThrow(new BusinessException("message")).when(this.mockLdapResource).create(ArgumentMatchers.any(UserOrgEditionVo.class));
		final InputStream input = new ByteArrayInputStream("Loubli;Sébastien;fdaugan;my.address@sample.com;gfi;jira".getBytes("cp1250"));
		initSpringSecurityContext(DEFAULT_USER);
		@SuppressWarnings("unchecked")
		final BatchTaskVo<UserImportEntry> importTask = (BatchTaskVo<UserImportEntry>) waitImport(
				resource.getImportTask(resource.full(input, new String[0], "cp1250")));
		Assertions.assertEquals("message", importTask.getEntries().get(0).getStatusText());
		Assertions.assertEquals(Boolean.FALSE, importTask.getEntries().get(0).getStatus());
	}

	@Test
	public void getImportTaskFailed() {
		Assertions.assertNull(resource.getImportTask(-1));
	}

	@Test
	public void getImportStatusFailed() {
		Assertions.assertNull(resource.getImportStatus(-1));
	}

	@Test
	public void getImportStatus() throws InterruptedException, IOException {
		final BatchTaskVo<UserImportEntry> importTask = full("Loubli;Sébastien;kloubli7;my.address@sample.com;gfi;,jira,");
		Assertions.assertSame(importTask, resource.getImportTask(importTask.getId()));
		Assertions.assertSame(importTask.getStatus(), resource.getImportStatus(importTask.getId()));
	}

	@Test
	public void getImportStatusPreviousFinished() throws InterruptedException, IOException {
		final BatchTaskVo<UserImportEntry> oldTask = full("Loubli;Sébastien;kloubli5a;my.address@sample.com;gfi;,jira,");
		oldTask.getStatus().setEnd(getDate(1980, 1, 1));
		final BatchTaskVo<UserImportEntry> importTask = full("Loubli;Sébastien;kloubli5b;my.address@sample.com;gfi;,jira,");
		Assertions.assertSame(importTask, resource.getImportTask(importTask.getId()));
		Assertions.assertSame(importTask.getStatus(), resource.getImportStatus(importTask.getId()));
	}

	@Test
	public void getImportStatusPreviousNotFinished() throws InterruptedException, IOException {
		final BatchTaskVo<UserImportEntry> oldTask = full("Loubli;Sébastien;kloubli6a;my.address@sample.com;gfi;,jira,");
		oldTask.getStatus().setEnd(null);
		final BatchTaskVo<UserImportEntry> importTask = full("Loubli;Sébastien;kloubli6b;my.address@sample.com;gfi;,jira,");
		Assertions.assertSame(importTask, resource.getImportTask(importTask.getId()));
		Assertions.assertSame(importTask.getStatus(), resource.getImportStatus(importTask.getId()));
		oldTask.getStatus().setEnd(new Date());
	}

	protected <U extends BatchElement> BatchTaskVo<U> full(final InputStream input, final String[] headers)
			throws IOException, InterruptedException {
		return full(input, headers, "cp1252");
	}

	protected <U extends BatchElement> BatchTaskVo<U> full(final InputStream input, final String[] headers, final String encoding)
			throws IOException, InterruptedException {
		initSpringSecurityContext(DEFAULT_USER);
		final long id = resource.full(input, headers, encoding);
		Assertions.assertNotNull(id);
		@SuppressWarnings("unchecked")
		final BatchTaskVo<U> importTask = (BatchTaskVo<U>) resource.getImportTask(id);
		Assertions.assertEquals(id, importTask.getId());
		return waitImport(importTask);
	}

	protected <U extends BatchElement> BatchTaskVo<U> full(final String csvData) throws IOException, InterruptedException {
		return full(csvData, "cp1252");
	}

	protected <U extends BatchElement> BatchTaskVo<U> full(final String csvData, final String encoding)
			throws IOException, InterruptedException {
		return full(new ByteArrayInputStream(csvData.getBytes(encoding)),
				new String[] { "lastName", "firstName", "id", "mail", "company", "groups" }, encoding);
	}

}
