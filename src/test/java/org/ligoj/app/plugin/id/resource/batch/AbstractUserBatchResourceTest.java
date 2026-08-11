/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import jakarta.transaction.Transactional;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.plugin.id.resource.UserOrgResource;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.mockito.ArgumentMatchers;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test of {@link UserOrgResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
abstract class AbstractUserBatchResourceTest extends AbstractBatchTest {

	protected UserOrgResource mockResource;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void mockApplicationContext() {
		final ApplicationContext applicationContext = mock(ApplicationContext.class);
		SpringUtils.setSharedApplicationContext(applicationContext);
		mockResource = mock(UserOrgResource.class);
		final UserFullTask mockTask = new UserFullTask();
		mockTask.resource = mockResource;
		mockTask.securityHelper = securityHelper;
		final UserAtomicTask mockTaskUpdate = new UserAtomicTask();
		mockTaskUpdate.resource = mockResource;
		mockTaskUpdate.securityHelper = securityHelper;
		when(applicationContext.getBean(SessionSettings.class)).thenReturn(new SessionSettings());
		when(applicationContext.getBean((Class<?>) ArgumentMatchers.any(Class.class))).thenAnswer(invocation -> {
			final Class<?> requiredType = (Class<Object>) invocation.getArguments()[0];
			if (requiredType == UserFullTask.class) {
				return mockTask;
			}
			if (requiredType == UserAtomicTask.class) {
				return mockTaskUpdate;
			}
			return AbstractUserBatchResourceTest.super.applicationContext.getBean(requiredType);
		});

		mockTaskUpdate.jaxrsFactory = ServerProviderFactory.createInstance(null);
	}

	@AfterEach
	void removeMockApplicationContext() {
		SpringUtils.setSharedApplicationContext(super.applicationContext);
	}

	@BeforeEach
	void prepareData() throws IOException {
		persistEntities("csv", new Class<?>[] { DelegateOrg.class }, StandardCharsets.UTF_8);
	}

}
