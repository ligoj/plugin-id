package org.ligoj.app.plugin.id.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.jaxrs.impl.MetadataMap;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.URITemplate;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.apache.cxf.message.ExchangeImpl;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.MultivaluedMap;

/**
 * Content negotiation of the container creation endpoints, as the JAX-RS runtime resolves it: the created
 * identifier is a raw string, so a client accepting anything, such as the web application, gets
 * <code>text/plain</code>, and only a client asking for JSON gets <code>application/json</code>.
 */
class ContainerCreateNegotiationTest {

	@Test
	void groupCreateAnyIsText() {
		Assertions.assertEquals(MediaType.TEXT_PLAIN, negotiate(GroupResource.class, "/service/id/group", "*/*"));
	}

	@Test
	void groupCreateJsonIsJson() {
		Assertions.assertEquals(MediaType.APPLICATION_JSON,
				negotiate(GroupResource.class, "/service/id/group", MediaType.APPLICATION_JSON));
	}

	@Test
	void companyCreateAnyIsText() {
		Assertions.assertEquals(MediaType.TEXT_PLAIN, negotiate(CompanyResource.class, "/service/id/company", "*/*"));
	}

	@Test
	void companyCreateJsonIsJson() {
		Assertions.assertEquals(MediaType.APPLICATION_JSON,
				negotiate(CompanyResource.class, "/service/id/company", MediaType.APPLICATION_JSON));
	}

	/**
	 * Return the response content type the runtime selects for a creation request sending this "Accept" header.
	 */
	private String negotiate(final Class<?> resource, final String path, final String accept) {
		final ClassResourceInfo cri = ResourceUtils.createClassResourceInfo(resource, resource, true, true);
		final Map<ClassResourceInfo, MultivaluedMap<String, String>> matched = new HashMap<>();
		final MultivaluedMap<String, String> values = new MetadataMap<>();
		values.putSingle(URITemplate.FINAL_MATCH_GROUP, "/");
		matched.put(cri, values);
		final Message message = new MessageImpl();
		message.setExchange(new ExchangeImpl());
		message.getExchange().put(Endpoint.class, Mockito.mock(Endpoint.class));
		message.put(Message.REQUEST_URI, path);
		Assertions.assertNotNull(JAXRSUtils.findTargetMethod(matched, message, "POST", new MetadataMap<>(),
				MediaType.APPLICATION_JSON, JAXRSUtils.parseMediaTypes(accept)));
		return List.of(((String) message.getExchange().get(Message.CONTENT_TYPE)).split(";")).getFirst();
	}
}
