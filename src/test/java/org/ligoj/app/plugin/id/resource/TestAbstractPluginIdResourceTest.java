/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.Arrays;
import java.util.Collections;

import javax.transaction.Transactional;
import javax.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link AbstractPluginIdResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class TestAbstractPluginIdResourceTest {

	@Autowired
	protected UserOrgResource userResource;

	private AbstractPluginIdResource<IUserRepository> resource;

	@BeforeEach
	public void init() {
		resource = new AbstractPluginIdResource<>() {

			@Override
			public boolean accept(Authentication authentication, String node) {
				return false;
			}

			@Override
			public String getKey() {
				return null;
			}

			@Override
			protected AbstractPluginIdResource<IUserRepository> getSelf() {
				return null;
			}

			@Override
			protected IUserRepository getUserRepository(String node) {
				return null;
			}
		};
		resource.userResource = userResource;
	}

	@Test
	public void toLogin() {
		final UserOrg user = new UserOrg();
		user.setFirstName("First");
		user.setLastName("Last123");
		Assertions.assertEquals("flast123", resource.toLogin(user));
	}

	@Test
	public void toLoginNoFirstName() {
		final UserOrg user = new UserOrg();
		user.setLastName("Last123");
		Assertions.assertThrows(NotAuthorizedException.class, () -> {
			resource.toLogin(user);
		});
	}

	@Test
	public void toLoginNoLastName() {
		final UserOrg user = new UserOrg();
		user.setFirstName("First");
		Assertions.assertThrows(NotAuthorizedException.class, () -> {
			resource.toLogin(user);
		});
	}

	@Test
	public void newApplicationUserSaveFail() {
		resource.userResource = Mockito.mock(UserOrgResource.class);
		Mockito.when(resource.userResource.findByIdNoCache("flast123")).thenReturn(null);
		Mockito.doThrow(new TechnicalException("")).when(resource.userResource)
				.saveOrUpdate(ArgumentMatchers.any(UserOrgEditionVo.class));

		final UserOrg user = new UserOrg();
		user.setMails(Collections.singletonList("fabrice.daugan@sample.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setName("secondarylogin");
		user.setCompany("ligoj");
		Assertions.assertThrows(TechnicalException.class, () -> {
			resource.newApplicationUser(user);
		});
	}

	@Test
	public void newApplicationUserNextLoginFail() {
		resource.userResource = Mockito.mock(UserOrgResource.class);
		Mockito.doThrow(new RuntimeException()).when(resource.userResource).findByIdNoCache("flast123");

		final UserOrg user = new UserOrg();
		user.setMails(Collections.singletonList("fabrice.daugan@sample.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setName("secondarylogin");
		user.setCompany("ligoj");
		Assertions.assertThrows(RuntimeException.class, () -> {
			resource.newApplicationUser(user);
		});
	}

	@Test
	public void toApplicationUserExists() {

		final UserOrgResource userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node plugged to the primary node
		final UserOrg existing = new UserOrg();
		existing.setId("primarylogin");
		existing.setMails(Collections.singletonList("marc.martin@sample.com"));
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setCompany("ligoj");
		existing.setDepartment("3890");
		existing.setLocalId("8234");

		final UserOrg authUser = new UserOrg();
		authUser.setId("secondarylogin");
		authUser.setMails(Collections.singletonList("marc.martin@sample.com"));
		authUser.setFirstName("Marc");
		authUser.setLastName("Martin");
		authUser.setCompany("ligoj");
		authUser.setDepartment("auth 3890");
		authUser.setLocalId("auth 8234");

		Mockito.doReturn(Collections.singletonList(existing)).when(userResource).findAllBy("mails",
				"marc.martin@sample.com");
		Mockito.doAnswer(invocation -> {
			TestAbstractPluginIdResourceTest.this.userResource.mergeUser(existing, authUser);
			return null;
		}).when(userResource).mergeUser(existing, authUser);
		Assertions.assertEquals("primarylogin", resource.toApplicationUser(authUser));
	}

	@Test
	public void toApplicationUserNew() {

		UserOrgResource userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;
		Mockito.doReturn(Collections.emptyList()).when(userResource).findAllBy("mails", "some@where.com");
		Mockito.doReturn(null).when(userResource).findByIdNoCache("flast123");
		Mockito.doAnswer(invocation -> {
			TestAbstractPluginIdResourceTest.this.userResource
					.saveOrUpdate((UserOrgEditionVo) invocation.getArguments()[0]);
			return null;
		}).when(userResource).saveOrUpdate(Mockito.any());

		// Create a new IAM node plugged to the primary node
		final UserOrg user = new UserOrg();
		user.setId("secondarylogin");
		user.setMails(Collections.singletonList("some@where.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setCompany("ligoj");
		Assertions.assertEquals("flast123", resource.toApplicationUser(user));
		Mockito.verify(userResource).saveOrUpdate(Mockito.any());
	}

	@Test
	public void toApplicationUserNewWithCollision() {
		final UserOrgResource userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node plugged to the primary node
		final UserOrg existing = new UserOrg();
		existing.setId("mmartin");
		existing.setMails(Collections.singletonList("marc.martin@where.com"));
		existing.setFirstName("Marc");
		existing.setLastName("Martin");
		existing.setCompany("ligoj");

		// Create a new IAM node plugged to the primary node
		final UserOrg authUser = new UserOrg();
		authUser.setId("mmartin");
		authUser.setMails(Collections.singletonList("some@where.com"));
		authUser.setFirstName("Marc");
		authUser.setLastName("Martin");
		authUser.setCompany("another-company");

		Mockito.doReturn(Collections.emptyList()).when(userResource).findAllBy("mails", "some@where.com");
		Mockito.doReturn(existing).when(userResource).findByIdNoCache("mmartin");
		Mockito.doReturn(null).when(userResource).findByIdNoCache("mmartin1");
		Mockito.doAnswer(invocation -> {
			TestAbstractPluginIdResourceTest.this.userResource
					.saveOrUpdate((UserOrgEditionVo) invocation.getArguments()[0]);
			return null;
		}).when(userResource).saveOrUpdate(Mockito.any());

		Assertions.assertEquals("mmartin1", resource.toApplicationUser(authUser));

		final UserOrg userIAM = this.userResource.findByIdNoCache("mmartin1");
		Assertions.assertEquals("mmartin1", userIAM.getName());
	}

	@Test
	public void toApplicationUserTooManyMail() {
		final UserOrgResource userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node pluged to the primary node
		final UserOrg existing = new UserOrg();
		existing.setMails(Collections.singletonList("marc.martin@sample.com"));
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setName("secondarylogin");

		Mockito.doReturn(Arrays.asList(existing, existing)).when(userResource).findAllBy("mails",
				"marc.martin@sample.com");

		Assertions.assertThrows(NotAuthorizedException.class, () -> {
			resource.toApplicationUser(existing);
		});
	}
}
