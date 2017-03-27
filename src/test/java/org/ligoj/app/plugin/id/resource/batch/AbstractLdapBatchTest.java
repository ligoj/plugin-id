package org.ligoj.app.plugin.id.resource.batch;

import org.junit.Assert;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class  AbstractLdapBatchTest extends AbstractAppTest {

	@Autowired
	protected SecurityHelper securityHelper;

	protected <U extends BatchElement> U checkImportTask(final BatchTaskVo<U> importTask) {
		Assert.assertNotNull(importTask);
		Assert.assertNotNull(importTask.getStatus().getEnd());
		Assert.assertEquals(1, importTask.getEntries().size());
		Assert.assertEquals(Boolean.TRUE, importTask.getStatus().getStatus());
		Assert.assertEquals(1, importTask.getStatus().getEntries());
		Assert.assertEquals(1, importTask.getStatus().getDone());
		return importTask.getEntries().get(0);
	}

	protected <U extends BatchElement> BatchTaskVo<U> waitImport(final BatchTaskVo<U> importTask) throws InterruptedException {
		Assert.assertNotNull(importTask);
		Assert.assertNotNull(importTask.getStatus().getStart());

		// Let the import to be proceeded
		for (int i = 1000; i-- > 0;) {
			Thread.sleep(10);
			if (importTask.getStatus().getEnd() != null) {
				// Import is finished
				Thread.sleep(100);
				break;
			}
		}
		return importTask;
	}

}
