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
        new JdbcTemplate(datasource).execute("create table task (id bigint primary key, value varchar(255), notes text)");
        new JdbcTemplate(datasource).execute("create table comment (id bigint primary key, company_id bigint not null, user_id bigint not null, content text not null)");
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
    }

    @BeforeEach void setup() {
        jdbc = new JdbcTemplate(datasource); repository = new MobileOfflineRepository(jdbc);
        transaction = new TransactionTemplate(new DataSourceTransactionManager(datasource));
        jdbc.update("delete from mobile_offline_receipt");
        jdbc.update("delete from mobile_offline_preparation");
        jdbc.update("delete from mobile_offline_device");
        jdbc.update("delete from task");
        jdbc.update("delete from comment");
        jdbc.update("insert into task(id,value,notes) values (7,'A','notes')");
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
}
