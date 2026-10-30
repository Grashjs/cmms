package com.grash.configuration;

import com.grash.model.*;
import com.grash.security.CustomUserDetail;
import com.grash.service.AnalyticsCacheService;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnalyticsCacheInvalidatorTest {

    @Mock
    private EntityManagerFactory entityManagerFactory;
    @Mock
    private AnalyticsCacheService analyticsCacheService;

    private AnalyticsCacheInvalidator invalidator;

    @BeforeEach
    void setUp() {
        invalidator = new AnalyticsCacheInvalidator(entityManagerFactory, analyticsCacheService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Nested
    class OutsideATransaction {

        @Test
        void workOrderChange_invalidatesItsCompany() {
            invalidator.onChange(workOrderOf(company(1L)));

            verify(analyticsCacheService).invalidateCompany(1L);
        }

        @Test
        void untrackedEntity_isIgnored() {
            Vendor vendor = new Vendor();
            vendor.setCompany(company(1L));

            invalidator.onChange(vendor);

            verifyNoInteractions(analyticsCacheService);
        }

        @Test
        void locationChange_invalidatesItsCompany() {
            // location filters include sub-locations
            Location location = new Location();
            location.setCompany(company(3L));

            invalidator.onChange(location);

            verify(analyticsCacheService).invalidateCompany(3L);
        }

        @Test
        void additionalCost_usesItsWorkOrdersCompany() {
            AdditionalCost cost = new AdditionalCost();
            cost.setWorkOrder(workOrderOf(company(4L)));

            invalidator.onChange(cost);

            verify(analyticsCacheService).invalidateCompany(4L);
        }

        @Test
        void reading_usesItsMetersCompany() {
            Meter meter = new Meter();
            meter.setCompany(company(5L));
            Reading reading = new Reading();
            reading.setMeter(meter);

            invalidator.onChange(reading);

            verify(analyticsCacheService).invalidateCompany(5L);
        }

        @Test
        void meterTriggerChange_invalidatesItsCompany() {
            // its value is a limit of the meter analytics
            WorkOrderMeterTrigger trigger = new WorkOrderMeterTrigger();
            trigger.setCompany(company(7L));

            invalidator.onChange(trigger);

            verify(analyticsCacheService).invalidateCompany(7L);
        }

        @Test
        void unknownCompany_fallsBackToTheAuthenticatedUsersCompany() {
            authenticateUserOf(company(6L));
            AdditionalCost cost = new AdditionalCost();
            cost.setWorkOrder(new WorkOrder()); // posted as {"workOrder": {"id": ...}}

            invalidator.onChange(cost);

            verify(analyticsCacheService).invalidateCompany(6L);
        }

        @Test
        void unknownCompanyAndNoUser_invalidatesEverything() {
            AdditionalCost cost = new AdditionalCost();
            cost.setWorkOrder(new WorkOrder());

            invalidator.onChange(cost);

            verify(analyticsCacheService).invalidateAll();
            verify(analyticsCacheService, never()).invalidateCompany(any());
        }
    }

    @Nested
    class InsideATransaction {

        @BeforeEach
        void beginTransaction() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @Test
        void invalidatesOnlyAfterCommit_oncePerCompany() {
            Company company1 = company(1L);
            invalidator.onChange(workOrderOf(company1));
            Labor labor = new Labor();
            labor.setCompany(company1);
            invalidator.onChange(labor);
            invalidator.onChange(workOrderOf(company(2L)));

            verifyNoInteractions(analyticsCacheService);

            commit();

            verify(analyticsCacheService, times(1)).invalidateCompany(1L);
            verify(analyticsCacheService, times(1)).invalidateCompany(2L);
            verifyNoMoreInteractions(analyticsCacheService);
        }

        @Test
        void rollback_doesNotInvalidate() {
            invalidator.onChange(workOrderOf(company(1L)));

            synchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

            verifyNoInteractions(analyticsCacheService);
        }

        private void commit() {
            synchronizations().forEach(TransactionSynchronization::afterCommit);
            synchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        }

        private List<TransactionSynchronization> synchronizations() {
            return TransactionSynchronizationManager.getSynchronizations();
        }
    }

    private static Company company(Long id) {
        Company company = new Company();
        company.setId(id);
        return company;
    }

    private static WorkOrder workOrderOf(Company company) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setCompany(company);
        return workOrder;
    }

    private static void authenticateUserOf(Company company) {
        User user = new User();
        user.setCompany(company);
        CustomUserDetail principal = CustomUserDetail.builder().user(user).build();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }
}
