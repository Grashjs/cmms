package com.grash.service;

import com.grash.dto.LaborPatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.offline.*;
import com.grash.exception.CustomException;
import com.grash.mapper.LaborMapper;
import com.grash.model.*;
import com.grash.model.enums.*;
import com.grash.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LaborServiceTest {
    @Mock LaborRepository repository;
    @Mock CompanyService companies;
    @Mock TimeCategoryService categories;
    @Mock UserService users;
    @Mock WorkOrderService workOrders;
    @Mock LaborMapper mapper;
    @Mock EntityManager em;
    @Mock LicenseService license;
    @Mock WorkOrderRepository workOrderRepository;
    @InjectMocks LaborService service;
    User user;
    WorkOrder wo;
    Labor primary;
    final Instant now = Instant.parse("2026-11-05T15:00:00Z");
    final Instant start = Instant.parse("2026-11-05T10:00:00Z");

    @BeforeEach void setup() {
        Company company = new Company(); company.setId(52L);
        var plan = new SubscriptionPlan(); plan.setFeatures(new HashSet<>(Set.of(PlanFeatures.ADDITIONAL_TIME)));
        Subscription subscription = new Subscription(); subscription.setSubscriptionPlan(plan); company.setSubscription(subscription);
        user = new User(); user.setId(34L); user.setCompany(company); user.setRate(20);
        var role = new Role(); role.setRoleType(RoleType.ROLE_CLIENT); user.setRole(role);
        wo = spy(new WorkOrder()); wo.setId(10L); wo.setCompany(company);
        lenient().doReturn(true).when(wo).canBeEditedBy(user);
        primary = new Labor(user, 20, Date.from(start.minusSeconds(3600)), wo, true, TimeStatus.STOPPED);
        primary.setId(100L); primary.setCompany(company); primary.setDuration(1800); primary.setVersion(4);
        lenient().when(workOrderRepository.findForUpdate(10L, 52L)).thenReturn(Optional.of(wo));
        lenient().when(repository.findByIdAndCompany_IdAndWorkOrder_Id(100L, 52L, 10L)).thenAnswer(c -> Optional.of(primary));
        lenient().when(repository.findByWorkOrder_Id(10L)).thenReturn(List.of(primary));
        lenient().when(repository.findById(100L)).thenAnswer(c -> Optional.of(primary));
        lenient().when(repository.saveAndFlush(any())).thenAnswer(c -> {
            Labor value = c.getArgument(0);
            if (value.getId() == null) { value.setId(101L); value.setCompany(company); }
            else value.setVersion(value.getVersion() + 1);
            return value;
        });
    }

    MobileOfflineOperation start(Long id, Long version, UUID previous) {
        return new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, null,
                MobileOfflineOperation.Type.START_TIMER, null, null, start, null, null, previous, null, null, null,
                new MobileOfflineTime(UUID.randomUUID(), start, null, id, version, null, 0L, null, null));
    }

    MobileOfflineOperation stop(MobileOfflineOperation source, long seconds) {
        return new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, null,
                MobileOfflineOperation.Type.STOP_TIMER, null, null, start.plusSeconds(seconds), null, null,
                source.operationId(), null, null, null,
                new MobileOfflineTime(source.time().periodId(), start, start.plusSeconds(seconds), null, null, null, null, null, source));
    }

    MobileOfflineResult receipt(MobileOfflineOperation operation, LaborService.TimeOutcome outcome, String code) {
        var labor = outcome.labor();
        return new MobileOfflineResult(operation.operationId(), MobileOfflineResult.Status.APPLIED, code, null, now,
                null, null, MobileOfflineResult.Visibility.VISIBLE, null, null, null, null,
                new MobileOfflineTimeResult(operation.time().periodId(), labor.getId(), labor.getVersion(), labor.getTimerRunId(),
                        operation.time().startedAt(), operation.time().endedAt(), outcome.durationAdded(), outcome.closedWithoutActivation()), null);
    }

    @Test void successiveClosedPeriodsAddDeltasToCurrentTotalWithoutOverlapRule() {
        var firstStart = start(100L, 4L, null);
        var firstStop = stop(firstStart, 1800);
        var first = service.applyOfflineTime(firstStop, user, null, null, true, now);
        assertNull(first.error()); assertEquals(3600, primary.getDuration());
        var previous = receipt(firstStop, first, "TIMER_PERIOD_RECORDED");
        // The second period deliberately overlaps the first: no new blocking business rule.
        var secondStart = start(100L, 4L, firstStop.operationId());
        var second = service.applyOfflineTime(stop(secondStart, 1200), user, previous, null, true, now);
        assertNull(second.error()); assertEquals(4800, primary.getDuration());
        assertEquals(26, primary.getCost());
        assertEquals(0, wo.getTimerStatusRevision());
        verifyNoInteractions(license);
    }

    @Test void connectedStartThenOfflineStopUsesFieldEndInsteadOfReceiptTime() {
        var begin = start(100L, 4L, null);
        var running = service.applyOfflineTime(begin, user, null, null, false, start);
        assertNull(running.error()); assertEquals(TimeStatus.RUNNING, primary.getStatus());
        assertEquals(Status.IN_PROGRESS, wo.getStatus()); assertEquals(Date.from(start), wo.getFirstTimeToReact());
        var stop = stop(begin, 1800);
        var stopped = service.applyOfflineTime(stop, user, null, receipt(begin, running, "TIMER_STARTED"), false, now);
        assertNull(stopped.error()); assertEquals(1800, stopped.durationAdded());
        assertEquals(3600, primary.getDuration()); assertEquals(TimeStatus.STOPPED, primary.getStatus());
        verifyNoInteractions(license);
    }

    @Test void delayedStopCannotStopARelancedGenerationOrDoubleAnExternalStop() {
        var begin = start(100L, 4L, null);
        var running = service.applyOfflineTime(begin, user, null, null, false, start);
        var accepted = receipt(begin, running, "TIMER_STARTED");
        UUID firstRun = primary.getTimerRunId();
        primary.setTimerRunId(UUID.randomUUID()); primary.setVersion(9);
        assertEquals("TIMER_RUN_CHANGED", service.applyOfflineTime(stop(begin, 1800), user, null, accepted, false, now).error());
        assertEquals(TimeStatus.RUNNING, primary.getStatus()); assertEquals(1800, primary.getDuration());
        primary.setTimerRunId(firstRun); primary.setStatus(TimeStatus.STOPPED);
        assertEquals("TIMER_ALREADY_STOPPED", service.applyOfflineTime(stop(begin, 1800), user, null, accepted, false, now).error());
        assertEquals(1800, primary.getDuration());
    }

    @Test void newTimerRequiresLicenseButExistingResumeDoesNot() {
        when(repository.findByWorkOrder_Id(10L)).thenReturn(List.of());
        assertEquals("TIME_TRACKING_REQUIRED", service.applyOfflineTime(start(null, null, null), user, null, null, false, now).error());
        var resumed = service.applyOfflineTime(start(100L, 4L, null), user, null, null, false, now);
        assertNull(resumed.error());
        verify(license, times(1)).hasEntitlement(LicenseEntitlement.TIME_TRACKING);
    }

    @Test void manualConditionsRemainSeparateAndClosedWorkOrderIsNotANewBan() {
        var request = new MobileOfflineOperation(UUID.randomUUID(), 52L, 34L, 10L, null,
                MobileOfflineOperation.Type.CREATE_MANUAL_TIME, null, null, now, null, null, null, null, null, null,
                new MobileOfflineTime(UUID.randomUUID(), start, start.plusSeconds(1200), null, null, null, null, null, null));
        assertEquals("TIME_TRACKING_REQUIRED", service.applyOfflineTime(request, user, null, null, false, now).error());
        when(license.hasEntitlement(LicenseEntitlement.TIME_TRACKING)).thenReturn(true);
        user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().clear();
        assertEquals("ADDITIONAL_TIME_REQUIRED", service.applyOfflineTime(request, user, null, null, false, now).error());
        user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().add(PlanFeatures.ADDITIONAL_TIME);
        wo.setStatus(Status.COMPLETE);
        var result = service.applyOfflineTime(request, user, null, null, false, now);
        assertNull(result.error()); assertEquals(1200, result.labor().getDuration());
        assertFalse(result.labor().isLogged()); assertSame(user, result.labor().getAssignedTo());
        assertEquals(Status.COMPLETE, wo.getStatus());
    }

    @Test void closedLocalDeltaPreservesAnExplicitlyReviewedOtherRunningGeneration() {
        primary.setStatus(TimeStatus.RUNNING); UUID run = UUID.randomUUID(); primary.setTimerRunId(run);
        Date serverStart = primary.getStartedAt();
        var begin = start(100L, 4L, null);
        var result = service.applyOfflineTime(stop(begin, 1800), user, null, null, true, now);
        assertNull(result.error()); assertEquals(3600, primary.getDuration());
        assertEquals(TimeStatus.RUNNING, primary.getStatus()); assertEquals(run, primary.getTimerRunId());
        assertEquals(serverStart, primary.getStartedAt());
    }

    @Test void statusDecisionRequiresReviewAndPreservingTheExaminedStatusDoesNotResume() {
        wo.setStatus(Status.ON_HOLD); wo.setTimerStatusRevision(1);
        var begin = start(100L, 4L, null);
        assertEquals("TIMER_STATUS_CHANGED", service.applyOfflineTime(stop(begin, 1800), user, null, null, true, now).error());
        var reviewedStart = new MobileOfflineOperation(begin.operationId(), 52L, 34L, 10L, null,
                begin.type(), null, null, start, null, null, null, null, null, null,
                new MobileOfflineTime(begin.time().periodId(), start, null, 100L, 4L, null, 1L, null, null));
        var stopped = stop(reviewedStart, 1800);
        stopped = new MobileOfflineOperation(stopped.operationId(), 52L, 34L, 10L, null, stopped.type(), null, null, now,
                null, null, reviewedStart.operationId(), null, null, null,
                new MobileOfflineTime(begin.time().periodId(), start, start.plusSeconds(1800), null, null, null, null, true, reviewedStart));
        assertNull(service.applyOfflineTime(stopped, user, null, null, true, now).error());
        assertEquals(Status.ON_HOLD, wo.getStatus()); assertEquals(1, wo.getTimerStatusRevision());
    }

    @Test void oldConnectedPatchAndDeleteCannotOverwriteAnUpdatedCumulativeTime() {
        when(repository.existsById(100L)).thenReturn(true);
        primary.setVersion(5); primary.setDuration(3600);
        LaborPatchDTO patch = new LaborPatchDTO(); patch.setVersion(4L); patch.setDuration(1800);
        assertEquals("LABOR_VERSION_CONFLICT", assertThrows(CustomException.class, () -> service.update(100L, patch, user)).getMessage());
        assertEquals("LABOR_VERSION_CONFLICT", assertThrows(CustomException.class, () -> service.delete(100L, 4L, user)).getMessage());
        assertEquals(3600, primary.getDuration()); verifyNoInteractions(mapper); verify(repository, never()).delete(any(Labor.class));
    }

    @Test void invalidDatesAndDeletedTargetPreserveTheCurrentTotal() {
        var begin = start(100L, 4L, null);
        assertEquals("INVALID_TIME_RANGE", service.applyOfflineTime(stop(begin, 0), user, null, null, true, now).error());
        when(repository.findByIdAndCompany_IdAndWorkOrder_Id(100L, 52L, 10L)).thenReturn(Optional.empty());
        assertEquals("LABOR_TARGET_CHANGED", service.applyOfflineTime(stop(begin, 1800), user, null, null, true, now).error());
        assertEquals(1800, primary.getDuration());
    }
}
