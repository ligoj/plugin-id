/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.function.Function;

import org.ligoj.bootstrap.resource.system.cache.CacheManagerAware;
import org.springframework.stereotype.Component;

import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.config.CacheConfig;

/**
 * Cache configuration for IAM.
 */
@Component
public class IdCache implements CacheManagerAware {

	@Override
	public void onCreate(final HazelcastCacheManager cacheManager, final Function<String, CacheConfig<?, ?>> provider) {
		cacheManager.createCache("container-scopes", provider.apply("container-scopes"));
		cacheManager.createCache("user-is-admin", provider.apply("user-is-admin"));
		cacheManager.createCache("id-configuration", provider.apply("id-configuration"));
	}

}
