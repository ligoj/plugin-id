/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

import javax.cache.Cache;
import javax.cache.configuration.CompleteConfiguration;
import javax.cache.expiry.ExpiryPolicy;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.ResourceOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.bootstrap.resource.system.cache.CronExpiryPolicy;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * In-memory identity data of a provider reading a remote server, with the database copy in the shared cache tables.
 * After a restart, the memory is rebuilt from that copy until the next full read is due, without reading the server.
 * The next full read is due at the expiry of the provider's cache after the last one: at the next time of its CRON
 * expression ({@code cache.<name>.cron}), or "last full read + TTL". With a CRON expression, the full reads run at its
 * times, nobody waits for them. A flush of the cache forces one.
 * <p>
 * A provider annotates {@link #ensureCachedData()} with {@code @CacheResult} and {@link #arm(long)} with
 * {@code @CachePut}, both on its cache: the single cache entry holds the time of the last full read.
 */
@Slf4j
public abstract class AbstractCopyMemCacheRepository extends AbstractMemCacheRepository implements SmartInitializingSingleton {

	/**
	 * The format of the time of the next full read, in the logs.
	 */
	private static final DateTimeFormatter NEXT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
			.withZone(ZoneId.systemDefault());

	/**
	 * The database copy of the identity data.
	 */
	@Autowired
	@Setter
	private IdCopy copy;

	/**
	 * Reads the expiry of the provider's cache.
	 */
	@Autowired
	private CacheManager cacheManager;

	/**
	 * Runs the full reads at the times of the CRON expression of the provider's cache, when it has one.
	 */
	@Autowired(required = false)
	@Setter
	private TaskScheduler scheduler;

	/**
	 * When the full read following a given one is due, from the expiry of the provider's cache, fixed when the cache is
	 * created: read once.
	 */
	private volatile UnaryOperator<Instant> schedule;

	/**
	 * The time, in milliseconds, of the full read the memory comes from.
	 */
	private volatile long lastFullRead;

	private final Object cacheLock = new Object();

	/**
	 * Ensure the data is in memory when the cache entry is missing, through {@link #ensureData()}. Annotated by the
	 * provider with {@code @CacheResult} on its cache.
	 *
	 * @return The time, in milliseconds, of the full read the memory comes from. Cached until the cache expires.
	 */
	public abstract long ensureCachedData();

	/**
	 * Replace the cache entry by the time of a new full read, so the expiry counts from it. Annotated by the provider with
	 * {@code @CachePut} on its cache, and the parameter with {@code @CacheValue}.
	 *
	 * @param lastFullRead The time, in milliseconds, of the full read.
	 */
	public abstract void arm(long lastFullRead);

	/**
	 * Return this repository through its proxy, so the cache annotations apply.
	 *
	 * @return The proxied repository.
	 */
	protected abstract AbstractCopyMemCacheRepository self();

	/**
	 * Return the name of the provider's cache: its expiry gives the times of the full reads.
	 *
	 * @return The cache name.
	 */
	protected abstract String getCacheName();

	/**
	 * Indicates the configuration is one of this provider: only then its data has a copy.
	 *
	 * @param configuration The IAM configuration of the primary node.
	 * @return <code>true</code> for a configuration of this provider.
	 */
	protected abstract boolean isProvider(IamConfiguration configuration);

	/**
	 * Return the fingerprint of the configuration shaping the data: another value means another data set.
	 *
	 * @param configuration The IAM configuration of the primary node.
	 * @return The fingerprint, at most 64 characters.
	 */
	protected abstract String fingerprint(IamConfiguration configuration);

	/**
	 * Indicates the last full read got all the data. An incomplete one, such as a listing stopped on a failed page, stays
	 * in memory but saves no state: the next restart reads the server again. By default, a full read is complete.
	 *
	 * @param configuration The IAM configuration of the primary node.
	 * @return <code>true</code> when the last full read is complete.
	 */
	protected boolean isComplete(final IamConfiguration configuration) {
		return true;
	}

	/**
	 * Rebuild the companies of this provider from their rows, as its full read builds them. By default, one company
	 * per row, its description holding its DN.
	 *
	 * @param configuration The IAM configuration of the primary node.
	 * @param rows          The company rows.
	 * @return The companies. Key is the normalized name.
	 */
	protected Map<String, CompanyOrg> toCompanies(final IamConfiguration configuration, final Collection<CacheCompany> rows) {
		final var companies = new HashMap<String, CompanyOrg>();
		for (final var row : rows) {
			final var company = new CompanyOrg(row.getDescription(), row.getName());
			companies.put(company.getId(), company);
		}
		return companies;
	}

	/**
	 * Return the identity data: from memory, rebuilt from the database copy after a restart, or read from the server
	 * when the copy is missing, the next full read is due, or the cache was flushed.
	 *
	 * @return The cached identity data.
	 */
	@Override
	public Map<CacheDataType, Map<String, ? extends ResourceOrg>> getData() {
		final var read = self().ensureCachedData();
		if (data != null && isDue(read)) {
			refreshData();
			self().arm(lastFullRead);
		}
		return Optional.ofNullable(data).orElseGet(this::restore);
	}

	/**
	 * The body of {@link #ensureCachedData()}: after a restart, the memory is rebuilt from the database copy when
	 * possible; otherwise, as after a cache flush, the server is read.
	 *
	 * @return The time, in milliseconds, of the full read the memory comes from.
	 */
	protected long ensureData() {
		if (data == null) {
			restore();
		} else {
			refreshData();
		}
		return lastFullRead;
	}

	/**
	 * Return when the full read following the given one is due: at the next time of the CRON expression of the
	 * provider's cache, or after its TTL.
	 *
	 * @param lastFullRead The time of a full read.
	 * @return The time of the next full read, {@link Instant#MAX} when the cache is eternal.
	 */
	public Instant getNextFullRead(final Instant lastFullRead) {
		if (schedule == null) {
			schedule = readSchedule();
		}
		return schedule.apply(lastFullRead);
	}

	/**
	 * Set the cache manager the expiry is read from.
	 *
	 * @param cacheManager The cache manager.
	 */
	public void setCacheManager(final CacheManager cacheManager) {
		this.cacheManager = cacheManager;
		this.schedule = null;
	}

	private ExpiryPolicy readPolicy() {
		final var cache = (Cache<?, ?>) cacheManager.getCache(getCacheName()).getNativeCache();
		final CompleteConfiguration<?, ?> configuration = cache.getConfiguration(CompleteConfiguration.class);
		return configuration.getExpiryPolicyFactory().create();
	}

	private UnaryOperator<Instant> readSchedule() {
		final var policy = readPolicy();
		if (policy instanceof CronExpiryPolicy cron) {
			return cron::next;
		}
		final var expiry = policy.getExpiryForCreation();
		if (expiry.isEternal()) {
			return _ -> Instant.MAX;
		}
		final var ttl = Duration.of(expiry.getDurationAmount(), expiry.getTimeUnit().toChronoUnit());
		return read -> read.plus(ttl);
	}

	private boolean isDue(final long read) {
		return !Instant.now().isBefore(getNextFullRead(Instant.ofEpochMilli(read)));
	}

	/**
	 * With a CRON expression on the provider's cache, schedule the full reads at its times: done in advance, nobody
	 * waits for them, and the copy is fresh for the restarts of the day.
	 */
	@Override
	public void afterSingletonsInstantiated() {
		if (scheduler != null && readPolicy() instanceof CronExpiryPolicy cron) {
			scheduler.schedule(this::fullReadOnSchedule, new CronTrigger(cron.getExpression(), cron.getZone()));
			log.info("Full reads of {} scheduled at '{}'", getCacheName(), cron.getExpression());
		}
	}

	/**
	 * The full read at a time of the CRON expression. Only the primary identity provider reads.
	 */
	private void fullReadOnSchedule() {
		if (isProvider(iamProvider[0].getConfiguration())) {
			refreshData();
			self().arm(lastFullRead);
		}
	}

	/**
	 * Update the user from the in-memory one: the given one may hold only the edited attributes, and every attribute
	 * is written to the copy.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void update(final UserOrg user) {
		super.update(Optional.ofNullable(data).map(d -> ((Map<String, UserOrg>) d.get(CacheDataType.USER)).get(user.getId()))
				.orElse(user));
	}

	/**
	 * Rebuild the memory from the database copy when it can be trusted, otherwise read the server.
	 */
	private Map<CacheDataType, Map<String, ? extends ResourceOrg>> restore() {
		synchronized (cacheLock) {
			if (data != null) {
				// Another caller just restored it
				return data;
			}
			final var start = System.currentTimeMillis();
			final var configuration = iamProvider[0].getConfiguration();
			final var snapshot = isProvider(configuration) ? loadCopy(configuration) : Optional.<IdCopy.Snapshot>empty();
			if (snapshot.isPresent()) {
				lastFullRead = snapshot.get().lastFullRead().toEpochMilli();
				data = snapshot.get().data();
				final var now = System.currentTimeMillis();
				log.info("Rebuilt identity data of {} from the database copy in {}, without reading the server: {}; last full read {} ago, {}",
						configuration.getNode(), DurationFormatUtils.formatDurationHMS(now - start), toStatistics(data),
						DurationFormatUtils.formatDurationHMS(now - lastFullRead), toNextFullRead(now));
				return data;
			}
		}
		return refreshData();
	}

	/**
	 * Load the database copy. A copy that cannot be read, such as a corrupted one, is never an error: the server is
	 * read.
	 */
	private Optional<IdCopy.Snapshot> loadCopy(final IamConfiguration configuration) {
		try {
			return copy.load(configuration.getNode(), fingerprint(configuration), Instant.now(), this::getNextFullRead,
					rows -> toCompanies(configuration, rows));
		} catch (final RuntimeException e) {
			log.warn("Full read required: unreadable copy", e);
			return Optional.empty();
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	protected Map<CacheDataType, Map<String, ? extends ResourceOrg>> refreshData() {
		final var refreshTime = cache.getCacheRefreshTime();
		final var read = lastFullRead;
		synchronized (cacheLock) {
			final var currentRefreshTime = cache.getCacheRefreshTime();
			final var now = System.currentTimeMillis();
			if (refreshTime != currentRefreshTime || read != lastFullRead) {
				// The shared tables or the whole full read, its state included, completed while this caller waited
				log.info("Another refresh just finished, new age is {}", DurationFormatUtils.formatDurationHMS(now - cache.getCacheRefreshTime()));
				return this.data;
			}
			if (refreshTime == 0) {
				log.info("Refresh cache requested, first time load");
			} else {
				log.info("Refresh cache requested, age is {}", DurationFormatUtils.formatDurationHMS(now - refreshTime));
			}

			// The reset invalidates the copy, until its state is written again
			final var configuration = iamProvider[0].getConfiguration();
			final var data = super.refreshData();
			cache.reset((Map<String, CompanyOrg>) data.get(CacheDataType.COMPANY),
					(Map<String, GroupOrg>) data.get(CacheDataType.GROUP), (Map<String, UserOrg>) data.get(CacheDataType.USER));
			if (isProvider(configuration) && isComplete(configuration)) {
				saveState(configuration, now);
			}
			lastFullRead = now;
			final var end = System.currentTimeMillis();
			log.info("Complete refresh of {} took {}: {}; {}", configuration.getNode(), DurationFormatUtils.formatDurationHMS(end - now),
					toStatistics(data), toNextFullRead(end));
			return data;
		}
	}

	/**
	 * Return the size of the data, for the logs.
	 */
	private static String toStatistics(final Map<CacheDataType, Map<String, ? extends ResourceOrg>> data) {
		return "users: " + data.get(CacheDataType.USER).size() + ", groups: " + data.get(CacheDataType.GROUP).size()
				+ ", companies: " + data.get(CacheDataType.COMPANY).size();
	}

	/**
	 * Return when the next full read is due, for the logs.
	 */
	private String toNextFullRead(final long now) {
		final var next = getNextFullRead(Instant.ofEpochMilli(lastFullRead));
		if (next.equals(Instant.MAX)) {
			return "no next one: the cache is eternal";
		}
		return "next one at " + NEXT_FORMAT.format(next) + ", in "
				+ DurationFormatUtils.formatDurationHMS(Math.max(0, next.toEpochMilli() - now));
	}

	/**
	 * Save the state of the copy of a full read. A state the database refuses is not an error: the fresh data stays in
	 * memory, and only the next restart reads the server.
	 */
	private void saveState(final IamConfiguration configuration, final long now) {
		try {
			copy.saveState(configuration.getNode(), fingerprint(configuration), Instant.ofEpochMilli(now));
		} catch (final RuntimeException e) {
			log.warn("The database copy could not be saved, the next restart reads the server", e);
		}
	}
}
