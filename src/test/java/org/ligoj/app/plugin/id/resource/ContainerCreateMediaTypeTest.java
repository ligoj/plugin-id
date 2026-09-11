package org.ligoj.app.plugin.id.resource;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Contract of the container creation endpoints: the created identifier is a raw string, so <code>text/plain</code>
 * comes first (a client accepting anything, such as the web application, gets the raw string), and
 * <code>application/json</code> is still produced so that clients asking only for JSON (the CLI, scripts) are not
 * refused with 406 as they were since the 5.0 rewrite.
 */
class ContainerCreateMediaTypeTest {

	@Test
	void companyCreateProducesTextAndJson() throws NoSuchMethodException {
		assertProduces(CompanyResource.class.getMethod("create", ContainerEditionVo.class).getAnnotation(Produces.class));
	}

	@Test
	void groupCreateProducesTextAndJson() throws NoSuchMethodException {
		assertProduces(GroupResource.class.getMethod("create", GroupEditionVo.class).getAnnotation(Produces.class));
	}

	private void assertProduces(final Produces produces) {
		Assertions.assertNotNull(produces);
		Assertions.assertEquals(List.of(MediaType.TEXT_PLAIN, MediaType.APPLICATION_JSON), List.of(produces.value()));
	}
}
