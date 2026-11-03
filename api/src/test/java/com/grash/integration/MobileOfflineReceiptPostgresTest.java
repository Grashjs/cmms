package com.grash.integration;

import com.grash.repository.MobileOfflineRepository;
import liquibase.Liquibase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.resource.ClassLoaderResourceAccessor;
import org.junit.jupiter.api.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.CannotAcquireLockException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Real PostgreSQL transactions, independent connections and the production Liquibase changeset. */
@Testcontainers(disabledWithoutDocker = true)
class MobileOfflineReceiptPostgresTest {
    @Container static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");
    static DriverManagerDataSource datasource;
    JdbcTemplate jdbc;
    MobileOfflineRepository repository;
    TransactionTemplate transaction;

    @BeforeAll static void schema() throws Exception {
        datasource = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        new JdbcTemplate(datasource).execute("create table file_transfer_fixture (id bigint primary key, path text not null)");
        new JdbcTemplate(datasource).execute("create table task (id bigint primary key, value varchar(255), notes text)");
        new JdbcTemplate(datasource).execute("create table comment (id bigint primary key, company_id bigint not null, user_id bigint not null, content text not null)");
        new JdbcTemplate(datasource).execute("create table work_order (id bigint primary key)");
        new JdbcTemplate(datasource).execute("create table labor (id bigint primary key, duration bigint not null, status integer not null, started_at timestamp with time zone not null)");
        new JdbcTemplate(datasource).execute("insert into work_order(id) values (10)");
        new JdbcTemplate(datasource).execute("insert into labor(id,duration,status,started_at) values (1000,1800,1,'2026-11-01 10:00:00+00')");
        try (var connection = datasource.getConnection();
             var liquibase = new Liquibase("db/changelog/2026_11_03_00000000001_mobile_offline_foundation.xml",
                     new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
            liquibase.update("");
        }
        try (var connection = datasource.getConnection();
             var liquibase = new Liquibase("db/changelog/2026_11_04_00000000001_mobile_offline_comments.xml",
                     new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
            liquibase.update("");
        }
        try (var connection = datasource.getConnection();
             var liquibase = new Liquibase("db/changelog/2026_11_05_00000000001_mobile_offline_time.xml",
                     new ClassLoaderResourceAccessor(), new JdbcConnection(connection))) {
            liquibase.update("");
        }
    }

    @BeforeEach void setup() {
        jdbc = new JdbcTemplate(datasource); repository = new MobileOfflineRepository(jdbc);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(datasource));
        jdbc.update("delete from mobile_offline_receipt");
        jdbc.update("delete from mobile_offline_preparation");
        jdbc.update("delete from mobile_offline_device");
        jdbc.update("delete from task");
        jdbc.update("delete from file_transfer_fixture");
        jdbc.update("delete from comment");
        jdbc.update("insert into task(id,value,notes) values (7,'A','notes')");
        jdbc.update("delete from labor where id<>1000");
        jdbc.update("insert into labor(id,duration,status,started_at) values (100,1800,0,'2026-11-01 08:00:00+00')");
    }

    @Test void responseLostThenNewConnectionAndReplayDoNotApplyAgain() {
        UUID id = UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            assertTrue(repository.reserveOperation(52, 34, id, 10, "hash", Instant.now()));
            jdbc.update("update task set value='B',version=version+1 where id=7");
            repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\"}");
        }); // Committed; pretend the HTTP response was lost.
        var reopened = new MobileOfflineRepository(new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())));
        assertTrue(reopened.receipt(52, 34, id).isPresent());
        transaction.executeWithoutResult(status -> assertFalse(repository.reserveOperation(52, 34, id, 10, "hash", Instant.now())));
        assertEquals(1L, jdbc.queryForObject("select version from task where id=7", Long.class));
        assertTrue(reopened.receipt(53, 34, id).isEmpty()); assertTrue(reopened.receipt(52, 35, id).isEmpty());
    }

    @Test void mutationAndReceiptRollbackTogether() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            repository.reserveOperation(52, 34, id, 10, "hash", Instant.now());
            jdbc.update("update task set value='B',version=version+1 where id=7");
            repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\"}");
            throw new IllegalStateException("crash before commit");
        }));
        assertTrue(repository.receipt(52, 34, id).isEmpty());
        assertEquals("A", jdbc.queryForObject("select value from task where id=7", String.class));
        transaction.executeWithoutResult(status -> {
            assertTrue(repository.reserveOperation(52, 34, id, 10, "hash", Instant.now()));
            repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\"}");
        });
    }

    @Test void commentAndReceiptCommitTogetherAndLostResponseNeverCreatesADuplicate() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            repository.reserveOperation(52, 34, id, 10, "hash", Instant.now());
            jdbc.update("insert into comment(id,company_id,user_id,content,source_operation_id) values (1,52,34,'field comment',?)", id);
            repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\",\"commentId\":1}");
            throw new IllegalStateException("rollback before receipt commit");
        }));
        assertEquals(0L, jdbc.queryForObject("select count(*) from comment", Long.class));
        assertTrue(repository.receipt(52, 34, id).isEmpty());
        transaction.executeWithoutResult(status -> {
            assertTrue(repository.reserveOperation(52, 34, id, 10, "hash", Instant.now()));
            jdbc.update("insert into comment(id,company_id,user_id,content,source_operation_id) values (1,52,34,'field comment',?)", id);
            repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\",\"commentId\":1}");
        });
        var reopened = new MobileOfflineRepository(new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())));
        assertTrue(reopened.receipt(52, 34, id).isPresent());
        transaction.executeWithoutResult(status -> assertFalse(reopened.reserveOperation(52, 34, id, 10, "hash", Instant.now())));
        assertEquals(1L, jdbc.queryForObject("select count(*) from comment", Long.class));
        assertThrows(org.springframework.dao.DataIntegrityViolationException.class, () ->
                jdbc.update("insert into comment(id,company_id,user_id,content,source_operation_id) values (2,52,34,'duplicate',?)", id));
        jdbc.update("insert into comment(id,company_id,user_id,content,source_operation_id) values (3,52,35,'other author',?)", id);
        jdbc.update("insert into comment(id,company_id,user_id,content) values (4,52,34,'connected'), (5,52,34,'connected again')");
        assertEquals(4L, jdbc.queryForObject("select count(*) from comment", Long.class));
    }

    @Test void simultaneousSubmissionsSerializeOnUniqueOperationId() throws Exception {
        UUID id = UUID.randomUUID();
        CountDownLatch firstReserved = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> transaction.execute(status -> {
                boolean reserved = repository.reserveOperation(52, 34, id, 10, "hash", Instant.now());
                firstReserved.countDown();
                try { assertTrue(releaseFirst.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                jdbc.update("update task set value='B',version=version+1 where id=7");
                repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\"}");
                return reserved;
            }));
            assertTrue(firstReserved.await(10, TimeUnit.SECONDS));
            Future<Boolean> second = pool.submit(() -> transaction.execute(status ->
                    repository.reserveOperation(52, 34, id, 10, "hash", Instant.now())));
            // No receipt is visible while a request can still commit: absence is not proof of non-application.
            assertTrue(repository.receipt(52, 34, id).isEmpty());
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS)); assertFalse(second.get(10, TimeUnit.SECONDS));
            assertEquals(1L, jdbc.queryForObject("select version from task where id=7", Long.class));
            assertNotNull(repository.receipt(52, 34, id).orElseThrow().resultJson());
        } finally { releaseFirst.countDown(); pool.shutdownNow(); }
    }

    @Test void transferFileAndReceiptCommitOrRollbackTogether() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            assertTrue(repository.reserveOperation(52, 34, id, 10, "binary-hash", Instant.now()));
            jdbc.update("insert into file_transfer_fixture(id,path) values (123,'company/owner/uuid/sha256')");
            repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\",\"fileId\":123}");
            throw new IllegalStateException("SQL commit failed after object upload");
        }));
        assertTrue(repository.receipt(52, 34, id).isEmpty());
        assertEquals(0L, jdbc.queryForObject("select count(*) from file_transfer_fixture", Long.class));
        transaction.executeWithoutResult(status -> {
            assertTrue(repository.reserveOperation(52, 34, id, 10, "binary-hash", Instant.now()));
            jdbc.update("insert into file_transfer_fixture(id,path) values (123,'company/owner/uuid/sha256')");
            repository.finishOperation(52, 34, id, "{\"status\":\"APPLIED\",\"fileId\":123}");
        });
        transaction.executeWithoutResult(status -> assertFalse(repository.reserveOperation(52, 34, id, 10, "binary-hash", Instant.now())));
        assertEquals(1L, jdbc.queryForObject("select count(*) from file_transfer_fixture", Long.class));
        assertTrue(repository.receipt(52, 34, id).orElseThrow().resultJson().contains("123"));
    }

    @Test void reservationReleaseAndOwnerIsolationAreDurable() {
        UUID device = UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            repository.lockDevice(52, 34, device);
            for (long id = 1; id <= 25; id++) repository.prepare(52, 34, device, id, Instant.now());
        });
        assertEquals(25, repository.preparedIds(52, 34, device).size());
        assertTrue(repository.preparedIds(52, 35, device).isEmpty());
        transaction.executeWithoutResult(status -> {
            repository.lockDevice(52, 34, device);
            repository.removePreparation(52, 34, device, 7);
        });
        assertEquals(24, repository.preparedIds(52, 34, device).size());
    }

    @Test void simultaneousPreparationsCannotExceedQuotaWithRepeatableReadSnapshots() throws Exception {
        UUID device = UUID.randomUUID();
        transaction.executeWithoutResult(status -> {
            repository.lockDevice(52, 34, device);
            for (long id = 1; id <= 24; id++) repository.prepare(52, 34, device, id, Instant.now());
        });
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> first = pool.submit(() -> transaction.execute(status -> {
                repository.lockDevice(52, 34, device);
                firstLocked.countDown();
                try { assertTrue(releaseFirst.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                assertEquals(24, repository.preparedIds(52, 34, device).size());
                repository.prepare(52, 34, device, 25, Instant.now());
                return true;
            }));
            assertTrue(firstLocked.await(10, TimeUnit.SECONDS));
            Future<Boolean> second = pool.submit(() -> {
                try {
                    return transaction.execute(status -> {
                        repository.lockDevice(52, 34, device);
                        if (repository.preparedIds(52, 34, device).size() >= 25) return false;
                        repository.prepare(52, 34, device, 26, Instant.now());
                        return true;
                    });
                } catch (CannotSerializeTransactionException | CannotAcquireLockException expected) { return false; }
            });
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            releaseFirst.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS)); assertFalse(second.get(10, TimeUnit.SECONDS));
            assertEquals(25, repository.preparedIds(52, 34, device).size());
        } finally { releaseFirst.countDown(); pool.shutdownNow(); }
    }

    @Test void timeMigrationIdentifiesLegacyRunningTimerWithoutChangingItsBusinessData() {
        assertNotNull(jdbc.queryForObject("select timer_run_id from labor where id=1000", UUID.class));
        assertEquals(0L, jdbc.queryForObject("select version from labor where id=1000", Long.class));
        assertEquals(1800L, jdbc.queryForObject("select duration from labor where id=1000", Long.class));
        assertEquals(1, jdbc.queryForObject("select status from labor where id=1000", Integer.class));
        assertEquals(Instant.parse("2026-11-01T10:00:00Z"), jdbc.queryForObject(
                "select started_at from labor where id=1000", java.sql.Timestamp.class).toInstant());
        assertEquals(0L, jdbc.queryForObject("select timer_status_revision from work_order where id=10", Long.class));
        assertNull(jdbc.queryForObject("select timer_run_id from labor where id=100", UUID.class));
    }

    private void applyClosedTime(UUID start, UUID stop) {
        assertTrue(repository.reserveOperation(52, 34, start, 10, "immutable-start", Instant.now()));
        assertTrue(repository.reserveOperation(52, 34, stop, 10, "immutable-stop", Instant.now()));
        jdbc.queryForObject("select duration from labor where id=100 for update", Long.class);
        jdbc.update("update labor set duration=duration+1800,version=version+1 where id=100");
        repository.finishOperation(52, 34, start, "{\"status\":\"APPLIED\",\"code\":\"TIMER_START_FINALIZED\"}");
        repository.finishOperation(52, 34, stop, "{\"status\":\"APPLIED\",\"code\":\"TIMER_PERIOD_RECORDED\"}");
    }

    @Test void closedTimeAndBothReceiptsRollbackTogetherThenReplayWithoutAnotherIncrement() {
        UUID start = UUID.randomUUID(), stop = UUID.randomUUID();
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            applyClosedTime(start, stop);
            throw new IllegalStateException("crash before pair commit");
        }));
        assertTrue(repository.receipt(52, 34, start).isEmpty());
        assertTrue(repository.receipt(52, 34, stop).isEmpty());
        assertEquals(1800L, jdbc.queryForObject("select duration from labor where id=100", Long.class));
        transaction.executeWithoutResult(status -> applyClosedTime(start, stop));
        var reopened = new MobileOfflineRepository(new JdbcTemplate(new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())));
        transaction.executeWithoutResult(status -> {
            assertFalse(reopened.reserveOperation(52, 34, start, 10, "immutable-start", Instant.now()));
            assertFalse(reopened.reserveOperation(52, 34, stop, 10, "immutable-stop", Instant.now()));
        });
        assertEquals(3600L, jdbc.queryForObject("select duration from labor where id=100", Long.class));
        assertEquals(0, jdbc.queryForObject("select status from labor where id=100", Integer.class));
        assertNotNull(reopened.receipt(52, 34, start).orElseThrow().resultJson());
        assertNotNull(reopened.receipt(52, 34, stop).orElseThrow().resultJson());
    }

    @Test void delayedStartWaitsForTheFinalizationAndCannotActivateAfterItsReceipt() throws Exception {
        UUID start = UUID.randomUUID(), stop = UUID.randomUUID();
        CountDownLatch reserved = new CountDownLatch(1), release = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> finalization = pool.submit(() -> transaction.executeWithoutResult(status -> {
                applyClosedTime(start, stop);
                reserved.countDown();
                try { assertTrue(release.await(10, TimeUnit.SECONDS)); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            }));
            assertTrue(reserved.await(10, TimeUnit.SECONDS));
            Future<Boolean> delayedStart = pool.submit(() -> transaction.execute(status -> {
                boolean first = repository.reserveOperation(52, 34, start, 10, "immutable-start", Instant.now());
                if (first) jdbc.update("update labor set status=1 where id=100");
                return first;
            }));
            assertThrows(TimeoutException.class, () -> delayedStart.get(100, TimeUnit.MILLISECONDS));
            release.countDown(); finalization.get(10, TimeUnit.SECONDS);
            assertFalse(delayedStart.get(10, TimeUnit.SECONDS));
            assertEquals(0, jdbc.queryForObject("select status from labor where id=100", Integer.class));
            assertEquals(3600L, jdbc.queryForObject("select duration from labor where id=100", Long.class));
        } finally { release.countDown(); pool.shutdownNow(); }
    }
}
