package com.grash.integration;

import com.grash.factory.MailServiceFactory;
import com.grash.mapper.CommentMapper;
import com.grash.repository.CommentRepository;
import com.grash.repository.UserRepository;
import com.grash.service.CommentService;
import com.grash.service.NotificationService;
import com.grash.service.WebhookDispatchService;
import com.grash.service.WorkOrderService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real Spring event/async proxies, with deterministic execution and no external delivery. */
@SpringJUnitConfig(CommentAfterCommitIntegrationTest.Config.class)
class CommentAfterCommitIntegrationTest {
    @Autowired ApplicationEventPublisher events;
    @Autowired PlatformTransactionManager transactions;
    @Autowired CommentService listener;
    @Autowired CommentRepository repository;
    @Autowired AtomicReference<Throwable> asyncFailure;
    CommentService target;

    @BeforeEach void setup() {
        target = AopTestUtils.getUltimateTargetObject(listener);
        clearInvocations(target);
        reset(repository);
        asyncFailure.set(null);
    }

    @Test void successfulTransactionDispatchesOnlyAfterCommit() {
        var event = new CommentService.OfflineCommentCreated(1, 52, 34);
        new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            verify(target, never()).afterOfflineCommentCommit(any());
        });
        verify(target).afterOfflineCommentCommit(event);
        verify(repository).findByIdAndCompany_Id(1L, 52L);
        assertNull(asyncFailure.get());
    }

    @Test void rollbackNeverDispatchesNotifications() {
        var event = new CommentService.OfflineCommentCreated(1, 52, 34);
        assertThrows(IllegalStateException.class, () -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            throw new IllegalStateException("receipt transaction rolled back");
        }));
        verify(target, never()).afterOfflineCommentCommit(any());
        verifyNoInteractions(repository);
    }

    @Test void publishingWithoutATransactionDoesNotSendUncommittedNotifications() {
        events.publishEvent(new CommentService.OfflineCommentCreated(1, 52, 34));
        verify(target, never()).afterOfflineCommentCommit(any());
        verifyNoInteractions(repository);
    }

    @Test void notificationFailureAfterCommitDoesNotFailTheAppliedTransaction() {
        var failure = new IllegalStateException("notification data unavailable after commit");
        when(repository.findByIdAndCompany_Id(1L, 52L)).thenThrow(failure);
        var event = new CommentService.OfflineCommentCreated(1, 52, 34);

        assertDoesNotThrow(() -> new TransactionTemplate(transactions).executeWithoutResult(status -> {
            events.publishEvent(event);
            verify(target, never()).afterOfflineCommentCommit(any());
        }));

        verify(target).afterOfflineCommentCommit(event);
        assertSame(failure, asyncFailure.get());
    }

    @Configuration
    @EnableTransactionManagement
    @EnableAsync
    static class Config implements AsyncConfigurer {
        // Keep the async interceptor (including its exception boundary), without
        // starting threads just to test the after-commit ordering.
        @Override public Executor getAsyncExecutor() { return new SyncTaskExecutor(); }
        @Override public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
            return (exception, method, args) -> asyncFailure().set(exception);
        }
        @Bean AtomicReference<Throwable> asyncFailure() { return new AtomicReference<>(); }
        @Bean CommentRepository repository() { return mock(CommentRepository.class); }
        @Bean PlatformTransactionManager transactionManager() {
            return new AbstractPlatformTransactionManager() {
                @Override protected Object doGetTransaction() { return new Object(); }
                @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
                @Override protected void doCommit(DefaultTransactionStatus status) {}
                @Override protected void doRollback(DefaultTransactionStatus status) {}
            };
        }
        @Bean CommentService listener(ApplicationEventPublisher events) {
            return spy(new CommentService(repository(), mock(CommentMapper.class), mock(WorkOrderService.class),
                    mock(EntityManager.class), mock(UserRepository.class), mock(NotificationService.class),
                    mock(MessageSource.class), mock(MailServiceFactory.class), mock(WebhookDispatchService.class), events));
        }
    }
}
