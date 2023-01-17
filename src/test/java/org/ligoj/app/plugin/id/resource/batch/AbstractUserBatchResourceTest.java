/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.transaction.Transactional;

import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.plugin.id.resource.UserOrgResource;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

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
		final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class);
		SpringUtils.setSharedApplicationContext(applicationContext);
		mockResource = Mockito.mock(UserOrgResource.class);
		final UserFullTask mockTask = new UserFullTask();
		mockTask.resource = mockResource;
		mockTask.securityHelper = securityHelper;
		final UserAtomicTask mockTaskUpdate = new UserAtomicTask();
		mockTaskUpdate.resource = mockResource;
		mockTaskUpdate.securityHelper = securityHelper;
		Mockito.when(applicationContext.getBean(SessionSettings.class)).thenReturn(new SessionSettings());
		Mockito.when(applicationContext.getBean((Class<?>) ArgumentMatchers.any(Class.class))).thenAnswer(invocation -> {
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
	void unmockApplicationContext() {
		SpringUtils.setSharedApplicationContext(super.applicationContext);
	}

	@BeforeEach
	void prepareData() throws IOException {
		persistEntities("csv", new Class[] { DelegateOrg.class }, StandardCharsets.UTF_8.name());
	}

}
