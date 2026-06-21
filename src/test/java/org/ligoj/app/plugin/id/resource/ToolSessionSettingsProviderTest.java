package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.api.NodeVo;
import org.ligoj.app.model.Node;
import org.ligoj.app.resource.node.NodeResource;
import org.ligoj.bootstrap.core.INamableBean;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.model.system.SystemConfiguration;
import org.ligoj.bootstrap.resource.system.configuration.ConfigurationResource;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.mockito.AdditionalAnswers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import jakarta.transaction.Transactional;

/**
 * Test of {@link ToolSessionSettingsProvider}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Transactional
@Rollback
class ToolSessionSettingsProviderTest extends AbstractAppTest {

	@BeforeEach
	void prepareData() throws IOException {
		// Only with Spring context
		persistEntities("csv", new Class<?>[] { SystemConfiguration.class, Node.class }, StandardCharsets.UTF_8);
		cacheManager.getCache("configuration").clear();

		// For the cache to be created
		getUser().findAll();
	}

	@Autowired
	private ToolSessionSettingsProvider provider;

	@Autowired
	private ConfigurationResource configuration;

	@SuppressWarnings("unchecked")
	@BeforeEach
	void mockApplicationContext() {
		final ApplicationContext applicationContext = Mockito.mock(ApplicationContext.class, AdditionalAnswers.delegatesTo(super.applicationContext));
		SpringUtils.setSharedApplicationContext(applicationContext);
		Mockito.doAnswer(invocation -> {
			final Class<?> requiredType = (Class<Object>) invocation.getArguments()[0];
			if (requiredType == SessionSettings.class) {
				return new SessionSettings();
			}
			return ToolSessionSettingsProviderTest.super.applicationContext.getBean(requiredType);
		}).when(applicationContext).getBean(ArgumentMatchers.any(Class.class));
	}

	@Test
	void decorate() {
		initSpringSecurityContext("fdaugan");
		final var details = new SessionSettings();
		details.setUserSettings(new HashMap<>());
		final var provider = new ToolSessionSettingsProvider();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(provider);
		provider.companyResource = Mockito.mock(CompanyResource.class);
		provider.nodeResource = Mockito.mock(NodeResource.class);
		final var node = new NodeVo();
		node.setId("service:km:confluence:dig");
		Mockito.when(provider.nodeResource.findAll()).thenReturn(Collections.singletonMap("service:km:confluence:dig", node));
		Mockito.when(provider.companyResource.isUserInternalCompany()).thenReturn(true);
		provider.decorate(details);
		Assertions.assertEquals(Boolean.TRUE, details.getUserSettings().get("internal"));
		@SuppressWarnings({ "unchecked", "rawtypes" })
		final List<Map<String, Object>> globalTools = (List) details.getUserSettings().get("globalTools");
		Assertions.assertEquals(1, globalTools.size());
		Assertions.assertEquals("service:km:confluence:dig", ((INamableBean<?>) globalTools.getFirst().get("node")).getId());
	}

	/**
	 * Invalid JSon in tool configuration.
	 */
	@Test
	void decorateError() {
		initSpringSecurityContext("fdaugan");
		final var details = new SessionSettings();
		details.setUserSettings(new HashMap<>());
		final var resource = new ToolSessionSettingsProvider();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(resource);
		configuration.put("global.tools.internal", "{error}");
		resource.decorate(details);
		Assertions.assertNull(details.getUserSettings().get("globalTools"));
	}

	@SuppressWarnings("rawtypes")
	@Test
	void decorateExternal() {
		initSpringSecurityContext("wuser");
		final var details = new SessionSettings();
		details.setUserSettings(new HashMap<>());
		final var provider = new ToolSessionSettingsProvider();
		applicationContext.getAutowireCapableBeanFactory().autowireBean(provider);
		provider.companyResource = Mockito.mock(CompanyResource.class);
		Mockito.when(provider.companyResource.isUserInternalCompany()).thenReturn(false);
		provider.decorate(details);
		Assertions.assertEquals(Boolean.TRUE, details.getUserSettings().get("external"));
		Assertions.assertTrue(((Collection) details.getUserSettings().get("globalTools")).isEmpty());
	}

	@Test
	void getKey() {
		Assertions.assertEquals("feature:menu:node", provider.getKey());
	}

	@Test
	void getInstalledEntities() {
		Assertions.assertTrue(provider.getInstalledEntities().contains(SystemConfiguration.class));
	}
}
