/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotAuthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.bootstrap.AbstractJpaTest;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Test class of {@link AbstractPluginIdResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
class TestAbstractPluginIdResourceTest extends AbstractJpaTest {

	private static final String SECONDARY_LOGIN = "secondary_login";
	private static final String PRIMARY_LOGIN = "flast123";
	@Autowired
	private UserOrgResource userResource;

	private AbstractPluginIdResource<IUserRepository> resource;

	private IUserRepository userRepository;

	@BeforeEach
	void init() {
		resource = new IUserRepositoryAbstractPluginIdResource();
		this.userRepository = Mockito.mock(IUserRepository.class);
		resource.userResource = userResource;
		cacheManager.getCache("id-configuration").clear();
	}

	@Test
	void refreshConfiguration() throws InterruptedException {
		final var deathCounter = new AtomicInteger();
		Mockito.when(TestAbstractPluginIdResourceTest.this.userRepository.getCompanyRepository()).thenAnswer(
				(Answer<ICompanyRepository>) invocation -> {
					Thread.sleep(200);
					if (deathCounter.incrementAndGet() > 2) {
						throw new RuntimeException("Expected lock exception for coverage");
					}
					return null;
				}
		);
		// Primary thread
		final var thread1 = new Thread(() -> {
			log.info("thread1 before refreshConfiguration");
			resource.refreshConfiguration("some:node");
			log.info("thread1 after refreshConfiguration");
		});
		thread1.start();

		// Concurrent thread (will wait)
		final var thread2 = new Thread(() -> {
			log.info("thread2 before refreshConfiguration");
			resource.refreshConfiguration("some:node");
			log.info("thread2 after refreshConfiguration");
		});
		thread2.start();

		// Exception thread
		final var thread3 = new Thread(() -> {
			log.info("thread3 before refreshConfiguration");
			resource.refreshConfiguration("some:node");
			log.info("thread3 after refreshConfiguration");
		});
		thread3.start();
		thread1.join();
		thread2.join();
		thread3.join();
		Assertions.assertEquals(3, deathCounter.get());
	}

	@Test
	void toLogin() {
		final var user = new UserOrg();
		user.setFirstName("First");
		user.setLastName("Last123");
		Assertions.assertEquals(PRIMARY_LOGIN, resource.toLogin(user));
	}

	@Test
	void toLoginNoFirstName() {
		final var user = new UserOrg();
		user.setLastName("Last123");
		Assertions.assertThrows(NotAuthorizedException.class, () -> resource.toLogin(user));
	}

	@Test
	void toLoginNoLastName() {
		final var user = new UserOrg();
		user.setFirstName("First");
		Assertions.assertThrows(NotAuthorizedException.class, () -> resource.toLogin(user));
	}

	@Test
	void newApplicationUserSaveFail() {
		resource.userResource = Mockito.mock(UserOrgResource.class);
		Mockito.when(resource.userResource.findByIdNoCache(PRIMARY_LOGIN)).thenReturn(null);
		Mockito.doThrow(new TechnicalException("")).when(resource.userResource)
				.saveOrUpdate(ArgumentMatchers.any(UserOrgEditionVo.class), Mockito.eq(true));

		final var user = new UserOrg();
		user.setMails(Collections.singletonList("fabrice.daugan@sample.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setName("secondaryLogin");
		user.setCompany("ligoj");
		Assertions.assertThrows(TechnicalException.class, () -> resource.newApplicationUser(user));
	}

	@Test
	void newApplicationUserNextLoginFail() {
		resource.userResource = Mockito.mock(UserOrgResource.class);
		Mockito.doThrow(new RuntimeException()).when(resource.userResource).findByIdNoCache(PRIMARY_LOGIN);

		final var user = new UserOrg();
		user.setMails(Collections.singletonList("fabrice.daugan@sample.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setName(SECONDARY_LOGIN);
		user.setCompany("ligoj");
		Assertions.assertThrows(RuntimeException.class, () -> resource.newApplicationUser(user));
	}

	@Test
	void toApplicationUserExists() {
		final var authentication = new UsernamePasswordAuthenticationToken(SECONDARY_LOGIN, null);
		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node plugged to the primary node
		final var existing = new UserOrg();
		existing.setId("primary_login");
		existing.setMails(Collections.singletonList("marc.martin@sample.com"));
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setCompany("ligoj");
		existing.setDepartment("3890");
		existing.setLocalId("8234");

		final var authUser = new UserOrg();
		authUser.setId(SECONDARY_LOGIN);
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
		Mockito.doReturn(authUser).when(userRepository).findOneBy("id", SECONDARY_LOGIN);
		Assertions.assertEquals("primary_login", resource.toApplicationUser(userRepository, authentication));
	}

	@Test
	void toApplicationUserNew() {
		final var authentication = new UsernamePasswordAuthenticationToken(SECONDARY_LOGIN, null);
		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;
		Mockito.doReturn(Collections.emptyList()).when(userResource).findAllBy("mails", "some@where.com");
		Mockito.doReturn(null).when(userResource).findByIdNoCache(PRIMARY_LOGIN);
		Mockito.doAnswer(invocation -> {
			TestAbstractPluginIdResourceTest.this.userResource
					.saveOrUpdate((UserOrgEditionVo) invocation.getArguments()[0], true);
			return null;
		}).when(userResource).saveOrUpdate(Mockito.any(), Mockito.eq(true));

		// Create a new IAM node plugged to the primary node
		final var user = new UserOrg();
		user.setId(SECONDARY_LOGIN);
		user.setMails(Collections.singletonList("some@where.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setCompany("ligoj");
		Mockito.doReturn(user).when(userRepository).findOneBy("id", SECONDARY_LOGIN);
		Assertions.assertEquals(PRIMARY_LOGIN, resource.toApplicationUser(userRepository, authentication));
		Mockito.verify(userResource).saveOrUpdate(Mockito.any(), Mockito.eq(true));
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
					.saveOrUpdate((UserOrgEditionVo) invocation.getArguments()[0], true);
			return null;
		}).when(userResource).saveOrUpdate(Mockito.any(), Mockito.eq(true));

		Mockito.doReturn(existing).when(userRepository).findOneBy("id", "mmartin");
		Assertions.assertEquals("mmartin1", resource.toApplicationUser(userRepository, authentication));

		final var userIAM = this.userResource.findByIdNoCache("mmartin1");
		Assertions.assertEquals("mmartin1", userIAM.getName());
	}

	@Test
	void toApplicationUserTooManyMail() {
		final var authentication = new UsernamePasswordAuthenticationToken(SECONDARY_LOGIN, null);
		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;

		// Create a new IAM node plugged to the primary node
		final var existing = new UserOrg();
		existing.setMails(Collections.singletonList("marc.martin@sample.com"));
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setName(SECONDARY_LOGIN);

		Mockito.doReturn(existing).when(userRepository).findOneBy("id", SECONDARY_LOGIN);
		Mockito.doReturn(Arrays.asList(existing, existing)).when(userResource).findAllBy("mails",
				"marc.martin@sample.com");

		Assertions.assertThrows(NotAuthorizedException.class, () -> resource.toApplicationUser(userRepository, authentication));
	}

	@Test
	void toApplicationUserNoMail() {

		// Create a new IAM node plugged to the primary node
		final var existing = new UserOrg();
		existing.setMails(Collections.emptyList());
		existing.setFirstName("First");
		existing.setLastName("Last123");
		existing.setName("login");

		final var authentication = new UsernamePasswordAuthenticationToken("login", null);
		Mockito.doReturn(existing).when(userRepository).findOneBy("id", "login");
		Assertions.assertThrows(NotAuthorizedException.class, () -> resource.toApplicationUser(userRepository, authentication));
	}

	@Test
	void getConfiguration() {
		final var configuration = resource.getConfiguration("service:id:test:node1");
		Assertions.assertSame(userRepository, configuration.getUserRepository());
	}

	@Test
	void authenticateFailed() {
		final var authentication = new UsernamePasswordAuthenticationToken(SECONDARY_LOGIN, null);
		Assertions.assertThrows(BadCredentialsException.class,
				() -> resource.authenticate(authentication, "service:id:test:node1", true));
	}

	@Test
	void authenticatePrimary() {
		final var authentication = new UsernamePasswordAuthenticationToken(SECONDARY_LOGIN, "secret");
		final var user =new UserOrg();
		user.setId(SECONDARY_LOGIN);
		Mockito.doReturn(user).when(userRepository).authenticate(SECONDARY_LOGIN, "secret");
		final var result = resource.authenticate(authentication, "service:id:test:node1", true);
		Assertions.assertEquals(SECONDARY_LOGIN, result.getName());
	}

	@Test
	void authenticateSecondary() {
		final var authentication = new UsernamePasswordAuthenticationToken(SECONDARY_LOGIN, "secret");
		Mockito.doReturn(new UserOrg()).when(userRepository).authenticate(SECONDARY_LOGIN, "secret");

		// Create a new IAM node plugged to the primary node
		final var user = new UserOrg();
		user.setId(SECONDARY_LOGIN);
		user.setMails(Collections.singletonList("some@where.com"));
		user.setFirstName("First");
		user.setLastName("Last123");
		user.setCompany("ligoj");
		Mockito.doReturn(user).when(userRepository).findOneBy("id", SECONDARY_LOGIN);

		final var userResource = Mockito.mock(UserOrgResource.class);
		resource.userResource = userResource;
		Mockito.doReturn(Collections.emptyList()).when(userResource).findAllBy("mails", "some@where.com");
		Mockito.doReturn(null).when(userResource).findByIdNoCache(PRIMARY_LOGIN);
		Mockito.doAnswer(invocation -> {
			TestAbstractPluginIdResourceTest.this.userResource
					.saveOrUpdate((UserOrgEditionVo) invocation.getArguments()[0], true);
			return null;
		}).when(userResource).saveOrUpdate(Mockito.any(), Mockito.eq(true));

		Assertions.assertEquals(PRIMARY_LOGIN, resource.authenticate(authentication, "service:id:test:node1", false).getPrincipal());
	}

	private class IUserRepositoryAbstractPluginIdResource extends AbstractPluginIdResource<IUserRepository> {

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
	}
}
