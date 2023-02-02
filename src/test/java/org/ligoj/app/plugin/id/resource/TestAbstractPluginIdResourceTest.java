/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.Arrays;
import java.util.Collections;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.bootstrap.AbstractJpaTest;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
public class TestAbstractPluginIdResourceTest extends AbstractJpaTest {

	@Autowired
	private UserOrgResource userResource;

	private AbstractPluginIdResource<IUserRepository> resource;

	private IUserRepository userRepository;

	@BeforeEach
	void init() {
		resource = new AbstractPluginIdResource<>() {

			@Override
			public boolean accept(Authentication authentication, String node) {
				return true;
			}

			@Override
			public String getKey() {
				return "service:id:test";
			}

			@Override
			protected AbstractPluginIdResource<IUserRepository> getSelf() {
				return this;
			}

			@Override
			protected IUserRepository getUserRepository(String node) {
				return TestAbstractPluginIdResourceTest.this.userRepository;
			}
		};
		this.userRepository = Mockito.mock(IUserRepository.class);
		resource.userResource = userResource;
		cacheManager.getCache("id-configuration").clear();
	}

	@Test
	void toLogin() {
		final var user = new UserOrg();
		user.setFirstName("First");
		user.setLastName("Last123");
		Assertions.assertEquals("flast123", resource.toLogin(user));
	}

	@Test
	void toLoginNoFirstName() {
		final var user = new UserOrg();
		user.setLastName("Last123");
		Assertions.assertThrows(NotAuthorizedException.class, () -> {
			resource.toLogin(user);
		});
	}

	@Test
	void toLoginNoLastName() {
		final var user = new UserOrg();
		user.setFirstName("First");
		Assertions.assertThrows(NotAuthorizedException.class, () -> {
			resource.toLogin(user);
		});
	}

	@Test
	void newApplicationUserSaveFail() {
		resource.userResource = Mockito.mock(UserOrgResource.class);
		Mockito.when(resource.userResource.findByIdNoCache("flast123")).thenReturn(null);
		Mockito.doThrow(new TechnicalException("")).when(resource.userResource)
				.saveOrUpdate(ArgumentMatchers.any(UserOrgEditionVo.class));

		final var user = new UserOrg();
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
	void newApplicationUserNextLoginFail() {
		resource.userResource = Mockito.mock(UserOrgResource.class);
		Mockito.doThrow(new RuntimeException()).when(resource.userResource).findByIdNoCache("flast123");

		final var user = new UserOrg();
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
	void toApplicationUserExists() {
		final var authentication = new UsernamePasswordAuthenticationToken("secondarylogin", null);
		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node plugged to the primary node
		final var existing = new UserOrg();
		existing.setId("primarylogin");
		existing.setMails(Collections.singletonList("marc.martin@sample.com"));
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setCompany("ligoj");
		existing.setDepartment("3890");
		existing.setLocalId("8234");

		final var authUser = new UserOrg();
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
		Mockito.doReturn(authUser).when(userRepository).findOneBy("id", "secondarylogin");
		Assertions.assertEquals("primarylogin", resource.toApplicationUser(userRepository, authentication));
	}

	@Test
	void toApplicationUserNew() {
		final var authentication = new UsernamePasswordAuthenticationToken("secondarylogin", null);
		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;
		Mockito.doReturn(Collections.emptyList()).when(userResource).findAllBy("mails", "some@where.com");
		Mockito.doReturn(null).when(userResource).findByIdNoCache("flast123");
		Mockito.doAnswer(invocation -> {
			TestAbstractPluginIdResourceTest.this.userResource
					.saveOrUpdate((UserOrgEditionVo) invocation.getArguments()[0]);
			return null;
		}).when(userResource).saveOrUpdate(Mockito.any());

		// Create a new IAM node plugged to the primary node
		final var user = new UserOrg();
		user.setId("secondarylogin");
		user.setMails(Collections.singletonList("some@where.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setCompany("ligoj");
		Mockito.doReturn(user).when(userRepository).findOneBy("id", "secondarylogin");
		Assertions.assertEquals("flast123", resource.toApplicationUser(userRepository, authentication));
		Mockito.verify(userResource).saveOrUpdate(Mockito.any());
	}

	@Test
	void toApplicationUserNewWithCollision() {
		final var authentication = new UsernamePasswordAuthenticationToken("mmartin", null);
		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node plugged to the primary node
		final var existing = new UserOrg();
		existing.setId("mmartin");
		existing.setMails(Collections.singletonList("marc.martin@where.com"));
		existing.setFirstName("Marc");
		existing.setLastName("Martin");
		existing.setCompany("ligoj");

		// Create a new IAM node plugged to the primary node
		final var authUser = new UserOrg();
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

		Mockito.doReturn(existing).when(userRepository).findOneBy("id", "mmartin");
		Assertions.assertEquals("mmartin1", resource.toApplicationUser(userRepository, authentication));

		final var userIAM = this.userResource.findByIdNoCache("mmartin1");
		Assertions.assertEquals("mmartin1", userIAM.getName());
	}

	@Test
	void toApplicationUserTooManyMail() {
		final var authentication = new UsernamePasswordAuthenticationToken("secondarylogin", null);
		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node pluged to the primary node
		final var existing = new UserOrg();
		existing.setMails(Collections.singletonList("marc.martin@sample.com"));
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setName("secondarylogin");

		Mockito.doReturn(existing).when(userRepository).findOneBy("id", "secondarylogin");
		Mockito.doReturn(Arrays.asList(existing, existing)).when(userResource).findAllBy("mails",
				"marc.martin@sample.com");

		Assertions.assertThrows(NotAuthorizedException.class, () -> {
			resource.toApplicationUser(userRepository, authentication);
		});
	}

	@Test
	void toApplicationUserNoMail() {

		// Create a new IAM node pluged to the primary node
		final var existing = new UserOrg();
		existing.setMails(Collections.emptyList());
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setName("login");

		final var authentication = new UsernamePasswordAuthenticationToken("login", null);
		Mockito.doReturn(existing).when(userRepository).findOneBy("id", "login");
		Assertions.assertThrows(NotAuthorizedException.class, () -> {
			resource.toApplicationUser(userRepository, authentication);
		});
	}

	@Test
	void getConfiguration() {
		final var configuration = resource.getConfiguration("service:id:test:node1");
		Assertions.assertSame(userRepository, configuration.getUserRepository());
	}

	@Test
	void authenticateFailed() {
		final var authentication = new UsernamePasswordAuthenticationToken("secondarylogin", null);
		Assertions.assertThrows(BadCredentialsException.class,
				() -> resource.authenticate(authentication, "service:id:test:node1", true));
	}

	@Test
	void authenticatePrimary() {
		final var authentication = new UsernamePasswordAuthenticationToken("secondarylogin", "secret");
		Mockito.doReturn(true).when(userRepository).authenticate("secondarylogin", "secret");
		Assertions.assertSame(authentication, resource.authenticate(authentication, "service:id:test:node1", true));
	}

	@Test
	void authenticateSecondary() {
		final var authentication = new UsernamePasswordAuthenticationToken("secondarylogin", "secret");
		Mockito.doReturn(true).when(userRepository).authenticate("secondarylogin", "secret");

		// Create a new IAM node plugged to the primary node
		final var user = new UserOrg();
		user.setId("secondarylogin");
		user.setMails(Collections.singletonList("some@where.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setCompany("ligoj");
		Mockito.doReturn(user).when(userRepository).findOneBy("id", "secondarylogin");

		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;
		Mockito.doReturn(Collections.emptyList()).when(userResource).findAllBy("mails", "some@where.com");
		Mockito.doReturn(null).when(userResource).findByIdNoCache("flast123");
		Mockito.doAnswer(invocation -> {
			TestAbstractPluginIdResourceTest.this.userResource
					.saveOrUpdate((UserOrgEditionVo) invocation.getArguments()[0]);
			return null;
		}).when(userResource).saveOrUpdate(Mockito.any());

		Assertions.assertEquals("flast123", resource.authenticate(authentication, "service:id:test:node1", false).getPrincipal());
	}
}
