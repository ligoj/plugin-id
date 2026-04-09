/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import com.hazelcast.cache.HazelcastCacheManager;
import org.ligoj.bootstrap.resource.system.cache.CacheConfigurer;
import org.ligoj.bootstrap.resource.system.cache.CacheManagerAware;
import org.springframework.stereotype.Component;

import javax.cache.expiry.Duration;

/**
 * Cache configuration for IAM.
 */
@Component
public class IdCache implements CacheManagerAware {

	@Override
	public void onCreate(final HazelcastCacheManager cacheManager, final CacheConfigurer configurer) {
		cacheManager.createCache("container-scopes", configurer.newCacheConfig("container-scopes"));
		final var isAdmin = configurer.newCacheConfig("user-is-admin",Duration.ONE_MINUTE);
		cacheManager.createCache("user-is-admin", isAdmin);
		cacheManager.createCache("id-configuration", configurer.newCacheConfig("id-configuration"));
	}

}
