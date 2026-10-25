/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import org.ligoj.app.plugin.id.model.CacheCopyState;
import org.ligoj.bootstrap.core.dao.RestRepository;

/**
 * {@link CacheCopyState} repository.
 */
public interface CacheCopyStateRepository extends RestRepository<CacheCopyState, String> {
	// All delegated to Spring Data
}
