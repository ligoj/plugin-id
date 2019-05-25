/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import org.junit.jupiter.api.Assertions;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base batch test.
 */
public abstract class AbstractBatchTest extends AbstractAppTest {

	@Autowired
	protected SecurityHelper securityHelper;

	protected <U extends BatchElement> U checkImportTask(final BatchTaskVo<U> importTask) {
		Assertions.assertNotNull(importTask);
		Assertions.assertNotNull(importTask.getStatus().getEnd());
		Assertions.assertEquals(1, importTask.getEntries().size());
		Assertions.assertEquals(Boolean.TRUE, importTask.getStatus().getStatus());
		Assertions.assertEquals(1, importTask.getStatus().getEntries());
		Assertions.assertEquals(1, importTask.getStatus().getDone());
		return importTask.getEntries().get(0);
	}

	protected <U extends BatchElement> BatchTaskVo<U> waitImport(final BatchTaskVo<U> importTask)
			throws InterruptedException {
		Assertions.assertNotNull(importTask);
		Assertions.assertNotNull(importTask.getStatus().getStart());

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
