/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id;

import javax.transaction.Transactional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.plugin.id.resource.IdentityResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Test class of {@link IdentityResource}
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class IdentityResourceTest extends AbstractAppTest {

	@Autowired
	private IdentityResource resource;

	@Test
	public void getKey() {
		Assertions.assertEquals("service:id", resource.getKey());
	}

	@Test
	public void getInstalledEntities() {
		Assertions.assertTrue(resource.getInstalledEntities().contains(Node.class));
		Assertions.assertTrue(resource.getInstalledEntities().contains(Parameter.class));
	}

}
