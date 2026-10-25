package com.grash.configuration;

import com.grash.model.*;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.security.CustomUserDetail;
import com.grash.service.AnalyticsCacheService;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.*;
import org.hibernate.persister.entity.EntityPersister;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Invalidates the analytics caches of a company once a transaction that changed data used by the reports commits,
 * so that completing a work order, logging labor or registering a downtime is reflected immediately instead of
 * after the cache TTL.
 * <p>
 * The company is captured while the entity is flushed (inside the transaction, where lazy associations can still
 * be read) and the invalidation is deferred to {@code afterCommit}: invalidating earlier would let a concurrent
 * request cache pre-commit data under the new generation. All changes of one transaction produce one invalidation
 * per company.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsCacheInvalidator implements PostInsertEventListener, PostUpdateEventListener,
        PostDeleteEventListener {

    private static final List<Class<?>> TRACKED_ENTITIES = List.of(
            WorkOrder.class,
            Request.class,
            PreventiveMaintenance.class,
            Labor.class,
            AdditionalCost.class,
            PartQuantity.class,
            PartTransaction.class,
            Part.class,
            Asset.class,
            AssetDowntime.class,
            Meter.class,
            Reading.class,
            // their values are the limits of the meter analytics
            WorkOrderMeterTrigger.class,
            WorkOrderCategory.class,
            // location filters include sub-locations, so moving a location changes filtered reports
            Location.class
    );

    private final EntityManagerFactory entityManagerFactory;
    private final AnalyticsCacheService analyticsCacheService;

    @PostConstruct
    void registerListeners() {
        EventListenerRegistry registry = entityManagerFactory.unwrap(SessionFactoryImplementor.class)
                .getServiceRegistry()
                .getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_INSERT, this);
        registry.appendListeners(EventType.POST_UPDATE, this);
        registry.appendListeners(EventType.POST_DELETE, this);
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        onChange(event.getEntity());
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        onChange(event.getEntity());
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        onChange(event.getEntity());
    }

    @Override
    public boolean requiresPostCommitHandling(EntityPersister persister) {
        return false;
    }

    void onChange(Object entity) {
        if (entity == null || TRACKED_ENTITIES.stream().noneMatch(type -> type.isInstance(entity))) return;
        Long companyId = resolveCompanyId(entity);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            currentTransactionEvictions().add(companyId);
        } else {
            invalidate(companyId);
        }
    }

    /**
     * @return the company owning the entity, or {@code null} when it cannot be determined (then every company's
     * analytics are invalidated, which is always safe).
     */
    private Long resolveCompanyId(Object entity) {
        Long companyId = null;
        if (entity instanceof CompanyAudit companyAudit) {
            companyId = companyIdOf(companyAudit);
        } else if (entity instanceof AdditionalCost additionalCost) {
            companyId = companyIdOf(additionalCost.getWorkOrder());
        } else if (entity instanceof Reading reading) {
            companyId = companyIdOf(reading.getMeter());
        }
        return companyId != null ? companyId : currentUserCompanyId();
    }

    /**
     * Only reads what is already in memory: entities posted with just an id ({"workOrder": {"id": 1}}) or
     * uninitialized proxies return {@code null} instead of triggering a load during the flush.
     */
    private static Long companyIdOf(CompanyAudit owner) {
        if (owner == null || !Hibernate.isInitialized(owner) || owner.getCompany() == null) return null;
        return owner.getCompany().getId();
    }

    private static Long currentUserCompanyId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetail userDetail)) {
            return null;
        }
        User user = userDetail.getUser();
        return user == null || user.getCompany() == null ? null : user.getCompany().getId();
    }

    private void invalidate(Long companyId) {
        if (companyId == null) analyticsCacheService.invalidateAll();
        else analyticsCacheService.invalidateCompany(companyId);
    }

    /**
     * One synchronization per transaction. Looked up among the registered synchronizations (rather than bound as a
     * resource) so that a REQUIRES_NEW transaction, whose synchronizations are suspended separately, gets its own.
     */
    private Set<Long> currentTransactionEvictions() {
        for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof EvictAfterCommit evictAfterCommit) return evictAfterCommit.companyIds;
        }
        EvictAfterCommit evictAfterCommit = new EvictAfterCommit();
        TransactionSynchronizationManager.registerSynchronization(evictAfterCommit);
        return evictAfterCommit.companyIds;
    }

    private final class EvictAfterCommit implements TransactionSynchronization {
        // may contain null, meaning "company unknown"
        private final Set<Long> companyIds = new HashSet<>();

        @Override
        public void afterCommit() {
            companyIds.forEach(AnalyticsCacheInvalidator.this::invalidate);
        }
    }
}
