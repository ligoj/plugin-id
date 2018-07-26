package org.ligoj.app.plugin.id.dao;

import java.util.Map;

import org.ligoj.app.iam.ResourceOrg;
import org.springframework.stereotype.Component;

@Component
public class SampleIdMemCacheRepository extends AbstractMemCacheRepository {

	@Override
	public Map<CacheDataType, Map<String, ? extends ResourceOrg>> getData() {
		return refreshData();
	}

}
