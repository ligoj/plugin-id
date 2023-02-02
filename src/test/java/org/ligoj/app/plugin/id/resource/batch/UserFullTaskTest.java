/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.ext.ExceptionMapper;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.model.ProviderInfo;
import org.apache.cxf.jaxrs.provider.ServerProviderFactory;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.plugin.id.resource.UserOrgResource;
import org.ligoj.bootstrap.AbstractSecurityTest;
import org.ligoj.bootstrap.core.resource.mapper.FailSafeExceptionMapper;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.mockito.Mockito;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * Test of {@link UserFullTask}
 */
class UserFullTaskTest extends AbstractSecurityTest {

	private UserFullTask task;

	@BeforeEach
	void setup() {
		task = new UserFullTask();
		task.resource = Mockito.mock(UserOrgResource.class);
		task.securityHelper = new SecurityHelper();
		task.jaxrsFactory = ServerProviderFactory.getInstance();
		initSpringSecurityContext(DEFAULT_USER);
	}

	@Test
	void runInvalidStatus() {
		final UserImportEntry entry = Mockito.mock(UserImportEntry.class);
		Mockito.when(entry.getId()).thenThrow(new RuntimeException());
		final BatchTaskVo<UserImportEntry> importTask = new BatchTaskVo<>();
		importTask.setEntries(Collections.singletonList(entry));
		task.configure(importTask);
		task.run();
		Assertions.assertEquals(Boolean.TRUE, importTask.getStatus().getStatus());
		Mockito.verify(entry, Mockito.atLeastOnce()).setStatus(Boolean.FALSE);
		Assertions.assertEquals(1, importTask.getStatus().getDone());
		Assertions.assertEquals(1, importTask.getStatus().getEntries());
	}

	@Test
	void run() {
		final BatchTaskVo<UserImportEntry> importTask = new BatchTaskVo<>();
		final UserImportEntry entry = new UserImportEntry();
		entry.setGroups(",group,");
		importTask.setEntries(Collections.singletonList(entry));
		task.configure(importTask);
		task.run();
		Assertions.assertEquals(Boolean.TRUE, importTask.getStatus().getStatus());
		Assertions.assertEquals(1, importTask.getStatus().getDone());
		Assertions.assertEquals(1, importTask.getStatus().getEntries());
	}

	@Test
	void configureMessage() throws IllegalArgumentException, IllegalAccessException {
		final ServerProviderFactory instance = ServerProviderFactory.getInstance();
		@SuppressWarnings("unchecked")
		final List<ProviderInfo<ExceptionMapper<?>>> object = (List<ProviderInfo<ExceptionMapper<?>>>) FieldUtils
				.getField(ServerProviderFactory.class, "exceptionMappers", true).get(instance);
		final FailSafeExceptionMapper provider = new FailSafeExceptionMapper();
		object.add(new ProviderInfo<>(provider, null, true));
		final JacksonJsonProvider jacksonJsonProvider = new JacksonJsonProvider();
		FieldUtils.getField(FailSafeExceptionMapper.class, "jacksonJsonProvider", true).set(provider, jacksonJsonProvider);

		final UserImportEntry entry = Mockito.mock(UserImportEntry.class);
		Mockito.when(entry.getId()).thenThrow(new RuntimeException());
		final BatchTaskVo<UserImportEntry> importTask = new BatchTaskVo<>();
		importTask.setEntries(Collections.singletonList(entry));
		task.configure(importTask);
		task.jaxrsFactory = instance;
		task.run();
		Assertions.assertEquals(Boolean.TRUE, importTask.getStatus().getStatus());
		Assertions.assertEquals(1, importTask.getStatus().getDone());
		Assertions.assertEquals(1, importTask.getStatus().getEntries());
	}

	@Test
	void configureMessage2() throws IllegalArgumentException {
		final UserImportEntry entry = Mockito.mock(UserImportEntry.class);
		Mockito.when(entry.getId()).thenThrow(new RuntimeException());
		final BatchTaskVo<UserImportEntry> importTask = new BatchTaskVo<>();
		importTask.setEntries(Collections.singletonList(entry));

		final Message message = Mockito.mock(Message.class);
		final UserFullTask task = new UserFullTask() {
			@Override
			protected Message getMessage() {
				return message;
			}

		};
		final Exchange exchange = Mockito.mock(Exchange.class);
		Mockito.when(message.getExchange()).thenReturn(exchange);
		final Endpoint endpoint = Mockito.mock(Endpoint.class);
		Mockito.when(exchange.getEndpoint()).thenReturn(endpoint);
		Mockito.when(endpoint.get("org.apache.cxf.jaxrs.provider.ServerProviderFactory")).thenReturn(ServerProviderFactory.getInstance());

		task.configure(importTask);
	}

}
