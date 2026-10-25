/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.dao;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.Collection;

import javax.cache.configuration.CompleteConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ExpiryPolicy;
import javax.cache.expiry.ModifiedExpiryPolicy;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.CompanyOrg;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.ResourceOrg;
import org.ligoj.app.iam.UserOrg;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.plugin.id.dao.AbstractMemCacheRepository.CacheDataType;
import org.ligoj.bootstrap.resource.system.cache.CronExpiryPolicy;
import org.mockito.ArgumentCaptor;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Test class of {@link AbstractCopyMemCacheRepository}: the restart from the database copy shared by the identity
 * plugins, without the JCache proxy.
 */
class CopyMemCacheRepositoryTest {

	/**
	 * A provider of the shared mechanism, its own when {@link #provider} is <code>true</code>.
	 */
	static class SampleCopyMemCacheRepository extends AbstractCopyMemCacheRepository {

		boolean provider = true;

		/**
		 * The completeness of the full read, the default one when <code>null</code>.
		 */
		Boolean complete;

		/**
		 * The cache entry when present: the method is not run.
		 */
		Long entry;

		@Override
		public long ensureCachedData() {
			return entry == null ? ensureData() : entry;
		}

		/**
		 * The last time put in the cache marker.
		 */
		Long armed;

		@Override
		public void arm(final long lastFullRead) {
			// The cache marker, in the real providers
			armed = lastFullRead;
		}

		@Override
		protected AbstractCopyMemCacheRepository self() {
			return this;
		}

		@Override
		protected String getCacheName() {
			return "id-sample-data";
		}

		@Override
		protected boolean isProvider(final IamConfiguration configuration) {
			return provider;
		}

		@Override
		protected String fingerprint(final IamConfiguration configuration) {
			return "fingerprint";
		}

		@Override
		protected boolean isComplete(final IamConfiguration configuration) {
			return complete == null ? super.isComplete(configuration) : complete;
		}
	}

	/**
	 * Every day at 03:00.
	 */
	private static final String NIGHTLY = "0 0 3 * * *";

	private static final Instant T = Instant.parse("2026-11-10T08:00:00Z");

	/**
	 * The time of the next full read in the logs.
	 */
	private static final String AT = "\\d{4}-\\d\\d-\\d\\d \\d\\d:\\d\\d:\\d\\d";

	private Map<String, GroupOrg> groups;
	private Map<String, CompanyOrg> companies;
	private Map<String, UserOrg> users;
	private SampleCopyMemCacheRepository repository;
	private IdCacheDao cache;
	private IdCopy copy;
	private javax.cache.Cache<Object, Object> nativeCache;

	@BeforeEach
	void init() {
		final var companyRepository = mock(ICompanyRepository.class);
		final var groupRepository = mock(IGroupRepository.class);
		final var userRepository = mock(IUserRepository.class);
		final var configuration = new IamConfiguration();
		configuration.setNode("service:id:sample:node");
		configuration.setCompanyRepository(companyRepository);
		configuration.setGroupRepository(groupRepository);
		configuration.setUserRepository(userRepository);
		final var iamProvider = mock(IamProvider.class);
		when(iamProvider.getConfiguration()).thenReturn(configuration);

		companies = new HashMap<>(Map.of("company", new CompanyOrg("dnc", "Company")));
		groups = new HashMap<>(Map.of("group", new GroupOrg("dn", "Group", new HashSet<>(Set.of("u")))));
		final var user = new UserOrg();
		user.setId("u");
		user.setCompany("company");
		user.setGroups(new ArrayList<>(List.of("group")));
		user.setLockedBy("admin");
		users = new HashMap<>(Map.of("u", user));
		when(companyRepository.findAllNoCache()).thenReturn(companies);
		when(groupRepository.findAllNoCache()).thenReturn(groups);
		when(userRepository.findAllNoCache(groups)).thenReturn(users);

		repository = new SampleCopyMemCacheRepository();
		repository.setIamProvider(new IamProvider[] { iamProvider });
		cache = mock(IdCacheDao.class);
		repository.setCache(cache);
		copy = mock(IdCopy.class);
		repository.setCopy(copy);
		repository.setCacheManager(cacheManager(Duration.ONE_DAY));
	}

	/**
	 * A cache manager whose cache expires after the given TTL.
	 */
	private CacheManager cacheManager(final Duration ttl) {
		return cacheManager(ModifiedExpiryPolicy.factoryOf(ttl));
	}

	/**
	 * A cache manager whose cache has the given expiry policy.
	 */
	@SuppressWarnings("unchecked")
	private CacheManager cacheManager(final Factory<? extends ExpiryPolicy> policy) {
		final var configuration = new MutableConfiguration<>().setExpiryPolicyFactory(policy);
		nativeCache = mock(javax.cache.Cache.class);
		when(nativeCache.getConfiguration(CompleteConfiguration.class)).thenReturn(configuration);
		final var springCache = mock(org.springframework.cache.Cache.class);
		when(springCache.getNativeCache()).thenReturn(nativeCache);
		final var manager = mock(CacheManager.class);
		when(manager.getCache("id-sample-data")).thenReturn(springCache);
		return manager;
	}

	private IdCopy.Snapshot snapshot() {
		final Map<CacheDataType, Map<String, ? extends ResourceOrg>> data = new EnumMap<>(CacheDataType.class);
		data.put(CacheDataType.COMPANY, companies);
		data.put(CacheDataType.GROUP, groups);
		data.put(CacheDataType.USER, users);
		return new IdCopy.Snapshot(data, Instant.now());
	}

	@Test
	void getDataFullRead() {
		Assertions.assertSame(users, repository.getData().get(CacheDataType.USER));
		verify(cache).reset(companies, groups, users);
		verify(copy).saveState(eq("service:id:sample:node"), eq("fingerprint"), any());
	}

	/**
	 * With a TTL, the next full read is due after the TTL.
	 */
	@Test
	void getNextFullRead() {
		Assertions.assertEquals(T.plus(java.time.Duration.ofDays(1)), repository.getNextFullRead(T));
	}

	/**
	 * The expiry is fixed when the cache is created: read once, not on each access.
	 */
	@Test
	void getNextFullReadReadOnce() {
		repository.getNextFullRead(T);
		repository.getNextFullRead(T);
		repository.getNextFullRead(T);
		verify(nativeCache, times(1)).getConfiguration(CompleteConfiguration.class);
	}

	@Test
	void getNextFullReadEternal() {
		repository.setCacheManager(cacheManager(Duration.ETERNAL));
		Assertions.assertEquals(Instant.MAX, repository.getNextFullRead(T));
	}

	/**
	 * A cache expiring at fixed times: the next full read is due at the first time after the last one.
	 */
	@Test
	void getNextFullReadCron() {
		repository.setCacheManager(cacheManager(CronExpiryPolicy.factoryOf(NIGHTLY)));
		Assertions.assertEquals(new CronExpiryPolicy(NIGHTLY).next(T), repository.getNextFullRead(T));
	}

	@Test
	void getDataFromCopy() {
		final var snapshot = snapshot();
		when(copy.load(any(), any(), any(), any(), any())).thenReturn(Optional.of(snapshot));
		Assertions.assertSame(snapshot.data(), repository.getData());
		Assertions.assertEquals(snapshot.lastFullRead().toEpochMilli(), ReflectionTestUtils.getField(repository, "lastFullRead"));
		final ArgumentCaptor<Function<Collection<CacheCompany>, Map<String, CompanyOrg>>> companies = ArgumentCaptor.captor();
		final ArgumentCaptor<UnaryOperator<Instant>> nextFullRead = ArgumentCaptor.captor();
		verify(copy).load(eq("service:id:sample:node"), eq("fingerprint"), any(), nextFullRead.capture(), companies.capture());
		verify(cache, never()).reset(any(), any(), any());

		// The copy expires with the cache
		Assertions.assertEquals(T.plus(java.time.Duration.ofDays(1)), nextFullRead.getValue().apply(T));

		// The companies are rebuilt by the provider
		final var row = new CacheCompany();
		row.setId("company");
		row.setName("Company");
		Assertions.assertEquals("Company", companies.getValue().apply(List.of(row)).get("company").getName());
	}

	/**
	 * Return the messages the shared mechanism logs while the action runs.
	 */
	private List<String> logs(final Runnable action) {
		final var messages = new ArrayList<String>();
		final var logger = (Logger) LogManager.getLogger(AbstractCopyMemCacheRepository.class);
		final var appender = new AbstractAppender("capture", null, null, true, Property.EMPTY_ARRAY) {
			@Override
			public void append(final LogEvent event) {
				messages.add(event.getMessage().getFormattedMessage());
			}
		};
		final var level = logger.getLevel();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.INFO);
		try {
			action.run();
		} finally {
			logger.removeAppender(appender);
			logger.setLevel(level);
			appender.stop();
		}
		return messages;
	}

	private static void assertLogged(final List<String> messages, final String regex) {
		Assertions.assertTrue(messages.stream().anyMatch(m -> m.matches(regex)), () -> regex + " not in " + messages);
	}

	/**
	 * The restore tells the server was not read, the size of the data, and when the next full read is due.
	 */
	@Test
	void getDataFromCopyLogsTheStatistics() {
		final var snapshot = new IdCopy.Snapshot(snapshot().data(), Instant.now().minus(java.time.Duration.ofHours(1)));
		when(copy.load(any(), any(), any(), any(), any())).thenReturn(Optional.of(snapshot));
		assertLogged(logs(repository::getData), "Rebuilt identity data of service:id:sample:node from the database copy in "
				+ "\\d\\d:\\d\\d:\\d\\d\\.\\d{3}, without reading the server: users: 1, groups: 1, companies: 1; "
				+ "last full read 01:00:0\\d\\.\\d{3} ago, next one at " + AT + ", in 2[23]:\\d\\d:\\d\\d\\.\\d{3}");
	}

	@Test
	void getDataFromCopyLogsEternalCache() {
		repository.setCacheManager(cacheManager(Duration.ETERNAL));
		when(copy.load(any(), any(), any(), any(), any())).thenReturn(Optional.of(snapshot()));
		assertLogged(logs(repository::getData), "Rebuilt identity data .*, no next one: the cache is eternal");
	}

	/**
	 * A full read tells the size of the data, and when the next one is due.
	 */
	@Test
	void getDataFullReadLogsTheStatistics() {
		assertLogged(logs(repository::getData), "Complete refresh of service:id:sample:node took \\d\\d:\\d\\d:\\d\\d\\.\\d{3}: "
				+ "users: 1, groups: 1, companies: 1; next one at " + AT + ", in (23:59:\\d\\d|24:00:00)\\.\\d{3}");
	}

	/**
	 * A restart keeping the cache entry, the memory lost: the copy is still used.
	 */
	@Test
	void getDataWithTheEntryKept() {
		repository.entry = System.currentTimeMillis();
		final var snapshot = snapshot();
		when(copy.load(any(), any(), any(), any(), any())).thenReturn(Optional.of(snapshot));
		Assertions.assertSame(snapshot.data(), repository.getData());
	}

	/**
	 * A cache flush with data in memory: the server is read.
	 */
	@Test
	void ensureDataWithMemoryReadsTheServer() {
		repository.getData();
		repository.ensureCachedData();
		verify(copy, times(2)).saveState(any(), any(), any());
	}

	@Test
	void refreshDataWithAge() {
		when(cache.getCacheRefreshTime()).thenReturn(1000L);
		Assertions.assertSame(users, repository.refreshData().get(CacheDataType.USER));
	}

	/**
	 * The companies rebuilt by default: one per row, its description is its DN.
	 */
	@Test
	void toCompanies() {
		final var row = new CacheCompany();
		row.setId("company");
		row.setName("Company");
		row.setDescription("ou=company");
		final var rebuilt = repository.toCompanies(new IamConfiguration(), List.of(row));
		Assertions.assertEquals("ou=company", rebuilt.get("company").getDn());
		Assertions.assertEquals("Company", rebuilt.get("company").getName());
	}

	/**
	 * A copy that cannot be read, such as a corrupted one: a full read instead, never an error.
	 */
	@Test
	void getDataUnreadableCopy() {
		when(copy.load(any(), any(), any(), any(), any())).thenThrow(new IllegalStateException("corrupted"));
		Assertions.assertNotNull(repository.getData().get(CacheDataType.USER).get("u"));
		verify(copy).saveState(any(), any(), any());
	}

	/**
	 * With a cache expiring at fixed times, the full read is due once a time passed since the last one.
	 */
	@Test
	void getDataDueByCron() {
		repository.setCacheManager(cacheManager(CronExpiryPolicy.factoryOf(NIGHTLY)));
		repository.getData();
		repository.entry = System.currentTimeMillis();
		repository.getData();
		verify(copy, times(1)).saveState(any(), any(), any());

		// Two days ago: a time passed since
		repository.entry = System.currentTimeMillis() - 2 * 24 * 3600 * 1000L;
		repository.getData();
		verify(copy, times(2)).saveState(any(), any(), any());
	}

	/**
	 * With a cache expiring at fixed times, the full reads run at these times, nobody waits for them.
	 */
	@Test
	void afterSingletonsInstantiatedSchedulesTheFullReads() {
		final var scheduler = mock(TaskScheduler.class);
		repository.setScheduler(scheduler);
		repository.setCacheManager(cacheManager(CronExpiryPolicy.factoryOf(NIGHTLY)));
		repository.afterSingletonsInstantiated();
		final ArgumentCaptor<Runnable> task = ArgumentCaptor.captor();
		final ArgumentCaptor<Trigger> trigger = ArgumentCaptor.captor();
		verify(scheduler).schedule(task.capture(), trigger.capture());
		Assertions.assertEquals(NIGHTLY, ((CronTrigger) trigger.getValue()).getExpression());

		// At a time: the full read, and the cache marker
		task.getValue().run();
		verify(cache).reset(companies, groups, users);
		verify(copy).saveState(eq("service:id:sample:node"), eq("fingerprint"), any());
		Assertions.assertEquals(ReflectionTestUtils.getField(repository, "lastFullRead"), repository.armed);
	}

	/**
	 * Another identity provider as the primary one: no full read at the times.
	 */
	@Test
	void scheduledFullReadNotProvider() {
		final var scheduler = mock(TaskScheduler.class);
		repository.setScheduler(scheduler);
		repository.setCacheManager(cacheManager(CronExpiryPolicy.factoryOf(NIGHTLY)));
		repository.afterSingletonsInstantiated();
		final ArgumentCaptor<Runnable> task = ArgumentCaptor.captor();
		verify(scheduler).schedule(task.capture(), any(Trigger.class));
		repository.provider = false;
		task.getValue().run();
		verify(cache, never()).reset(any(), any(), any());
		Assertions.assertNull(repository.armed);
	}

	/**
	 * With a TTL, the full reads stay on demand.
	 */
	@Test
	void afterSingletonsInstantiatedTtl() {
		final var scheduler = mock(TaskScheduler.class);
		repository.setScheduler(scheduler);
		repository.afterSingletonsInstantiated();
		verifyNoInteractions(scheduler);
	}

	/**
	 * Without a scheduler, the full reads stay on demand.
	 */
	@Test
	void afterSingletonsInstantiatedNoScheduler() {
		repository.setCacheManager(cacheManager(CronExpiryPolicy.factoryOf(NIGHTLY)));
		repository.afterSingletonsInstantiated();
		Assertions.assertNull(repository.armed);
	}

	@Test
	void getDataDue() {
		// A TTL of zero: each access finds the full read due
		repository.setCacheManager(cacheManager(Duration.ZERO));
		repository.getData();
		verify(copy, times(2)).saveState(any(), any(), any());
	}

	@Test
	void restoreOnceForConcurrentCallers() throws InterruptedException {
		final var loading = new CountDownLatch(1);
		final var release = new CountDownLatch(1);
		final var snapshot = snapshot();
		when(copy.load(any(), any(), any(), any(), any())).thenAnswer(i -> {
			loading.countDown();
			release.await();
			return Optional.of(snapshot);
		});
		final var first = new Thread(repository::getData);
		first.start();
		loading.await();

		// The second caller waits for the lock held by the first one
		final var second = new Thread(repository::getData);
		second.start();
		waitBlocked(second);
		release.countDown();
		first.join();
		second.join();
		verify(copy, times(1)).load(any(), any(), any(), any(), any());
	}

	private static void waitBlocked(final Thread thread) {
		final var deadline = System.currentTimeMillis() + 10_000;
		while (thread.getState() != Thread.State.BLOCKED && System.currentTimeMillis() < deadline) {
			Thread.onSpinWait();
		}
	}

	/**
	 * A request arriving while the state of a full read is saved waits for it, and does not read the server again.
	 */
	@Test
	void refreshDataOnceWhileTheStateIsSaved() throws InterruptedException {
		final var refreshTime = new AtomicLong(0);
		when(cache.getCacheRefreshTime()).thenAnswer(i -> refreshTime.get());
		doAnswer(i -> refreshTime.incrementAndGet()).when(cache).reset(any(), any(), any());
		final var saving = new CountDownLatch(1);
		final var release = new CountDownLatch(1);
		doAnswer(i -> {
			saving.countDown();
			release.await();
			return null;
		}).when(copy).saveState(any(), any(), any());
		final var first = new Thread(repository::refreshData);
		first.start();
		saving.await();
		final var second = new Thread(repository::refreshData);
		second.start();
		waitBlocked(second);
		release.countDown();
		first.join();
		second.join();
		verify(copy, times(1)).saveState(any(), any(), any());
	}

	@Test
	void refreshDataSequential() {
		final var data = repository.refreshData();
		Assertions.assertNotSame(data, repository.refreshData());
	}

	@Test
	void refreshDataWithConflict() {
		final var conflictDate = new AtomicLong(0);
		when(cache.getCacheRefreshTime()).thenAnswer(a -> conflictDate.incrementAndGet());
		final var data = repository.refreshData();
		Assertions.assertSame(data, repository.refreshData());
	}

	/**
	 * A state the database refuses is not saved, and the fresh data stays in memory.
	 */
	@Test
	void refreshDataWhenTheStateCannotBeSaved() {
		doThrow(new IllegalStateException("refused")).when(copy).saveState(any(), any(), any());
		Assertions.assertSame(users, repository.refreshData().get(CacheDataType.USER));
		Assertions.assertNotEquals(0L, ReflectionTestUtils.getField(repository, "lastFullRead"));
	}

	/**
	 * An incomplete full read, such as a listing stopped on a failed page, stays in memory but saves no state: the
	 * reset forgot the previous one, so the next restart reads the server again.
	 */
	@Test
	void refreshDataIncomplete() {
		repository.complete = false;
		Assertions.assertSame(users, repository.refreshData().get(CacheDataType.USER));
		verify(cache).reset(companies, groups, users);
		verify(copy, never()).saveState(any(), any(), any());
	}

	/**
	 * Another identity provider as the primary one: no copy, as before.
	 */
	@Test
	void getDataNotProvider() {
		repository.provider = false;
		Assertions.assertNotNull(repository.getData());
		verifyNoInteractions(copy);
	}

	/**
	 * An update gives only the edited attributes: the in-memory user, holding all of them, is written.
	 */
	@Test
	void updateWritesTheMemoryUser() {
		repository.getData();
		final var edited = new UserOrg();
		edited.setId("u");
		repository.update(edited);
		verify(cache).update(users.get("u"));
	}

	@Test
	void updateUnknownUser() {
		repository.getData();
		final var unknown = new UserOrg();
		unknown.setId("unknown");
		repository.update(unknown);
		verify(cache).update(unknown);
	}

	@Test
	void updateWithoutMemory() {
		final var edited = new UserOrg();
		edited.setId("u");
		repository.update(edited);
		verify(cache).update(edited);
	}
}
