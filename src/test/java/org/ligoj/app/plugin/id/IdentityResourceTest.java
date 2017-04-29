package org.ligoj.app.plugin.id;

import javax.transaction.Transactional;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.plugin.id.resource.IdentityResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Test class of {@link IdentityResource}
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
public class IdentityResourceTest extends AbstractAppTest {

	@Autowired
	private IdentityResource resource;

	@Test
	public void getKey() {
		Assert.assertEquals("service:id", resource.getKey());
	}

	@Test
	public void getInstalledEntities() {
		Assert.assertTrue(resource.getInstalledEntities().contains(Node.class));
		Assert.assertTrue(resource.getInstalledEntities().contains(Parameter.class));
	}

}
