package org.ligoj.app.plugin.id.resource.batch;

import java.util.ArrayList;
import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.DefaultVerificationMode;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.plugin.id.resource.UserOrgEditionVo;
import org.ligoj.app.plugin.id.resource.UserOrgResource;
import org.ligoj.bootstrap.AbstractSecurityTest;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.mockito.exceptions.base.MockitoException;

/**
 * Test of {@link UserAtomicTask}
 */
public class UserAtomicTaskTest extends AbstractSecurityTest {

	private UserAtomicTask task;

	@BeforeEach
	public void setup() {
		task = new UserAtomicTask();
		task.resource = Mockito.mock(UserOrgResource.class);
		task.securityHelper = new SecurityHelper();
		initSpringSecurityContext(DEFAULT_USER);

		final UserOrg user = new UserOrg();
		user.setCompany("untouched");
		user.setDepartment("untouched");
		user.setFirstName("untouched");
		user.setLastName("untouched");
		user.setName(DEFAULT_USER);
		user.setLocalId("untouched");
		user.setMails(new ArrayList<>());
		user.setGroups(new ArrayList<>());
		Mockito.when(task.resource.findById(DEFAULT_USER)).thenReturn(user);
	}

	@Test
	public void doBatchInvalidOperation() {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("any");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			task.doBatch(entry);
		}),  "operation", "unsupported-operation");
	}

	@Test
	public void doBatchExtraValue() {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("lock");
		entry.setValue("any");
		MatcherUtil.assertThrows(Assertions.assertThrows(ValidationJsonException.class, () -> {
			task.doBatch(entry);
		}), "value", "null-value-expected");
	}

	@Test
	public void doBatchDepartment() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("department");
		entry.setValue("value");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);
		checkAttribute(UserOrgEditionVo::getDepartment, "value");
	}

	@Test
	public void doBatchLastName() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("lastname");
		entry.setValue("value");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);
		checkAttribute(UserOrgEditionVo::getLastName, "value");
	}

	@Test
	public void doBatchCompany() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("company");
		entry.setValue("value");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);
		checkAttribute(UserOrgEditionVo::getCompany, "value");
	}

	@Test
	public void doBatchLocalId() throws Exception {
		// Only for coverage
		UserBatchUpdateType.valueOf(UserBatchUpdateType.values()[0].name());

		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("localid");
		entry.setValue("value");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);
		checkAttribute(UserOrgEditionVo::getLocalId, "value");
	}

	@Test
	public void doBatchMail() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("mail");
		entry.setValue("value");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);
		checkAttribute(UserOrgEditionVo::getMail, "value");
	}

	@Test
	public void doBatchFirstName() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("firstname");
		entry.setValue("value");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);
		checkAttribute(UserOrgEditionVo::getFirstName, "value");
	}

	@Test
	public void doBatchIsolate() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("isolate");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);

		// Check user
		Mockito.verify(task.resource).isolate(DEFAULT_USER);
	}

	@Test
	public void doBatchRestore() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("restore");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);

		// Check user
		Mockito.verify(task.resource).restore(DEFAULT_USER);
	}

	@Test
	public void doBatchLock() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("lock");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);

		// Check user
		Mockito.verify(task.resource).lock(DEFAULT_USER);
	}

	@Test
	public void doBatchDelete() throws Exception {
		final UserUpdateEntry entry = new UserUpdateEntry();
		entry.setOperation("delete");
		entry.setUser(DEFAULT_USER);
		task.doBatch(entry);

		// Check user
		Mockito.verify(task.resource).delete(DEFAULT_USER);
	}

	private void checkAttribute(final Function<UserOrgEditionVo, String> function, final String value) {

		// Check user
		Mockito.verify(task.resource, new DefaultVerificationMode(data -> {
			if (data.getAllInvocations().size() != 2) {
				throw new MockitoException("Expect two calls");
			}

			// "findBy" call
			Assertions.assertEquals(DEFAULT_USER, data.getAllInvocations().get(0).getArguments()[0]);

			// "update" call
			final UserOrgEditionVo user = (UserOrgEditionVo) data.getAllInvocations().get(1).getArguments()[0];
			Assertions.assertNotNull(user);
			Assertions.assertEquals(value, function.apply(user));
		})).update(null);

	}

}
