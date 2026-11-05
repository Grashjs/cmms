package com.grash.service;

import com.grash.dto.LaborPatchDTO;
import com.grash.dto.license.LicenseEntitlement;
import com.grash.dto.offline.*;
import com.grash.exception.CustomException;
import com.grash.mapper.LaborMapper;
import com.grash.model.Labor;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.enums.TimeStatus;
import com.grash.model.enums.Status;
import com.grash.model.enums.PlanFeatures;
import com.grash.model.enums.RoleType;
import com.grash.repository.LaborRepository;
import com.grash.repository.WorkOrderRepository;
import com.grash.utils.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.time.Instant;
import java.time.Duration;
import java.util.UUID;
import java.util.Objects;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LaborService {
    private final LaborRepository laborRepository;

    private final CompanyService companyService;
    private final TimeCategoryService timeCategoryService;
    private final UserService userService;
    private final WorkOrderService workOrderService;
    private final LaborMapper laborMapper;
    private final EntityManager em;
    private final LicenseService licenseService;
    private final WorkOrderRepository workOrderRepository;

    @Transactional
    public Labor create(Labor labor) {
        if (!licenseService.hasEntitlement(LicenseEntitlement.TIME_TRACKING))
            throw new CustomException("You need a license to create a labor", HttpStatus.FORBIDDEN);
        updateHourlyRateIfNeeded(labor);
        Labor savedLabor = laborRepository.saveAndFlush(labor);
        em.refresh(savedLabor);
        return savedLabor;
    }

    private void updateHourlyRateIfNeeded(Labor labor) {
        if (labor.getHourlyRate() <= 0L && labor.getAssignedTo() != null) {
            User assignedUser =
                    userService.findById(labor.getAssignedTo().getId()).orElseThrow(() -> new CustomException("User " +
                            "not found", HttpStatus.NOT_FOUND));
            if (assignedUser.getRate() > 0L) labor.setHourlyRate(assignedUser.getRate());
        }
    }

    @Transactional
    public Labor update(Long id, LaborPatchDTO labor) {
        return update(id, labor, null);
    }

    @Transactional
    public Labor update(Long id, LaborPatchDTO labor, User user) {
        if (laborRepository.existsById(id)) {
            Labor savedLabor = laborRepository.findById(id).get();
            WorkOrder wo = lockTarget(savedLabor.getWorkOrder().getId(), savedLabor.getCompany().getId());
            if (user != null && !wo.canBeEditedBy(user)) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
            em.refresh(savedLabor);
            checkVersion(savedLabor, labor.getVersion());
            Labor labor1 = laborMapper.updateLabor(savedLabor, labor);
            updateHourlyRateIfNeeded(labor1);
            Labor updatedLabor = laborRepository.saveAndFlush(labor1);
            em.refresh(updatedLabor);
            return updatedLabor;
        } else throw new CustomException("Not found", HttpStatus.NOT_FOUND);
    }

    public Labor save(Labor labor) {
        return laborRepository.save(labor);
    }

    public Collection<Labor> getAll() {
        return laborRepository.findAll();
    }

    public void delete(Long id) {
        laborRepository.deleteById(id);
    }

    @Transactional
    public void delete(Long id, Long version, User user) {
        Labor labor = findById(id).orElseThrow(() -> new CustomException("Labor not found", HttpStatus.NOT_FOUND));
        WorkOrder wo = lockTarget(labor.getWorkOrder().getId(), user.getCompany().getId());
        if (!wo.canBeEditedBy(user)) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        em.refresh(labor);
        checkVersion(labor, version);
        laborRepository.delete(labor);
    }

    public Optional<Labor> findById(Long id) {
        return laborRepository.findById(id);
    }

    public Collection<Labor> findByWorkOrder(Long id) {
        return laborRepository.findByWorkOrder_Id(id);
    }

    @Transactional
    public Labor stop(Labor labor) {
        // System status changes use this path too; the work-order lock is shared
        // with offline writes and connected corrections.
        lockTarget(labor.getWorkOrder().getId(), labor.getCompany().getId());
        em.refresh(labor);
        if (labor.getStatus() != null && labor.getStatus().equals(TimeStatus.STOPPED))
            return labor;
        ensureRunId(labor);
        labor.setStatus(TimeStatus.STOPPED);
        labor.setDuration(labor.getDuration() + Helper.getDateDiff(labor.getStartedAt(), new Date(), TimeUnit.SECONDS));
        return laborRepository.saveAndFlush(labor);
    }

    @Transactional
    public Labor controlTimer(Long workOrderId, boolean start, Long laborId, UUID timerRunId, Long version, User user) {
        WorkOrder wo = lockTarget(workOrderId, user.getCompany().getId());
        if (!wo.canBeEditedBy(user)) throw new CustomException("Not found", HttpStatus.NOT_FOUND);
        if (start) {
            Date now = new Date();
            applyStartEffects(wo, now.toInstant(), false);
            Labor labor = primary(wo.getId(), user).stream().findFirst().orElse(null);
            if (labor == null) {
                labor = new Labor(user, user.getRate(), now, wo, true, TimeStatus.RUNNING);
                labor.setTimerRunId(UUID.randomUUID());
                return create(labor); // Existing creation license, deliberately not imposed on resume.
            }
            em.refresh(labor);
            if (labor.getStatus() == TimeStatus.RUNNING) {
                ensureRunId(labor);
                return laborRepository.saveAndFlush(labor);
            }
            labor.setStartedAt(now);
            labor.setStatus(TimeStatus.RUNNING);
            labor.setTimerRunId(UUID.randomUUID());
            return laborRepository.saveAndFlush(labor);
        }
        if (laborId == null || timerRunId == null) throw new CustomException("TIMER_RUN_CHANGED", HttpStatus.CONFLICT);
        Labor labor = ownedPrimary(laborId, wo, user);
        if (labor == null) throw new CustomException("LABOR_TARGET_CHANGED", HttpStatus.CONFLICT);
        checkVersion(labor, version);
        if (!timerRunId.equals(labor.getTimerRunId())) throw new CustomException("TIMER_RUN_CHANGED", HttpStatus.CONFLICT);
        return stop(labor);
    }

    @Transactional
    public Labor createConnected(Labor request, User user) {
        if (!hasManualPlan(user)) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        WorkOrder wo = lockTarget(request.getWorkOrder().getId(), user.getCompany().getId());
        if (!wo.canBeEditedBy(user)) throw new CustomException("Access denied", HttpStatus.FORBIDDEN);
        request.setWorkOrder(wo);
        if (wo.getFirstTimeToReact() == null) wo.setFirstTimeToReact(new Date());
        return create(request);
    }

    public static boolean hasManualPlan(User user) {
        return user.getCompany() != null && user.getCompany().getSubscription() != null
                && user.getCompany().getSubscription().getSubscriptionPlan() != null
                && user.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ADDITIONAL_TIME);
    }

    public static boolean hasManualRole(User user) {
        return user.getRole() != null && user.getRole().getRoleType() == RoleType.ROLE_CLIENT;
    }

    private WorkOrder lockTarget(Long id, Long company) {
        WorkOrder wo = workOrderRepository.findForUpdate(id, company)
                .orElseThrow(() -> new CustomException("TARGET_NOT_FOUND", HttpStatus.NOT_FOUND));
        // A caller may already have changed this managed work order (status stop).
        // Query AUTO flush preserves that mutation before refreshing the locked row.
        em.refresh(wo);
        return wo;
    }

    private List<Labor> primary(Long id, User user) {
        return findByWorkOrder(id).stream().filter(labor -> labor.isLogged() && labor.getAssignedTo() != null
                && Objects.equals(labor.getAssignedTo().getId(), user.getId())).toList();
    }

    private Labor ownedPrimary(Long id, WorkOrder wo, User user) {
        Labor labor = laborRepository.findByIdAndCompany_IdAndWorkOrder_Id(id, user.getCompany().getId(), wo.getId()).orElse(null);
        if (labor == null || !labor.isLogged() || labor.getAssignedTo() == null
                || !Objects.equals(labor.getAssignedTo().getId(), user.getId())) return null;
        em.refresh(labor);
        return labor;
    }

    private static void checkVersion(Labor labor, Long version) {
        if (version == null) throw new CustomException("LABOR_VERSION_REQUIRED", HttpStatus.CONFLICT);
        if (labor.getVersion() != version) throw new CustomException("LABOR_VERSION_CONFLICT", HttpStatus.CONFLICT);
    }

    private void ensureRunId(Labor labor) {
        // Migration assigns these for pre-existing running clocks. The fallback
        // also supports an old row created by an import/test without resetting time.
        if (labor.getTimerRunId() == null) labor.setTimerRunId(UUID.randomUUID());
    }

    private void applyStartEffects(WorkOrder wo, Instant start, boolean preserveStatus) {
        if (wo.getFirstTimeToReact() == null) wo.setFirstTimeToReact(Date.from(start));
        if (!preserveStatus) wo.setStatus(Status.IN_PROGRESS);
        workOrderRepository.save(wo);
    }

    public record TimeOutcome(Labor labor, String error, boolean conflict, long durationAdded, boolean closedWithoutActivation) {
        static TimeOutcome rejected(String code) { return new TimeOutcome(null, code, false, 0, false); }
        static TimeOutcome conflict(String code) { return new TimeOutcome(null, code, true, 0, false); }
    }

    /** Caller owns the receipt transaction. All expected refusals precede any mutation. */
    public TimeOutcome applyOfflineTime(MobileOfflineOperation operation, User user,
                                       MobileOfflineResult predecessor, MobileOfflineResult acceptedStart,
                                       boolean closeWithoutStart, Instant now) {
        return applyOfflineTime(operation, user, predecessor, acceptedStart, closeWithoutStart, now, null);
    }

    public TimeOutcome applyOfflineTime(MobileOfflineOperation operation, User user,
                                       MobileOfflineResult predecessor, MobileOfflineResult acceptedStart,
                                       boolean closeWithoutStart, Instant now,
                                       com.grash.dto.offline.MobileOfflineStatusResult statusPredecessor) {
        MobileOfflineTime time = operation.time();
        MobileOfflineOperation startOperation = time.startOperation();
        boolean manual = operation.type() == MobileOfflineOperation.Type.CREATE_MANUAL_TIME;
        boolean starting = operation.type() == MobileOfflineOperation.Type.START_TIMER;
        MobileOfflineTime base = closeWithoutStart ? startOperation.time() : time;
        Instant startedAt = time.startedAt();
        try {
            if (!Date.from(startedAt).toInstant().equals(startedAt)
                    || (time.endedAt() != null && !Date.from(time.endedAt()).toInstant().equals(time.endedAt())))
                return TimeOutcome.rejected("INVALID_TIME_RANGE");
        } catch (IllegalArgumentException invalid) { return TimeOutcome.rejected("INVALID_TIME_RANGE"); }
        if (startedAt.isAfter(now.plusSeconds(120)) || (time.endedAt() != null && time.endedAt().isAfter(now.plusSeconds(120))))
            return TimeOutcome.rejected("INVALID_TIME_RANGE");
        long seconds = 0;
        if (!starting) {
            try { seconds = Duration.between(startedAt, time.endedAt()).getSeconds(); }
            catch (ArithmeticException invalid) { return TimeOutcome.rejected("INVALID_TIME_RANGE"); }
            if (seconds <= 0 || seconds > Integer.MAX_VALUE) return TimeOutcome.rejected("INVALID_TIME_RANGE");
        }
        WorkOrder wo = workOrderRepository.findForUpdate(operation.workOrderId(), user.getCompany().getId()).orElse(null);
        if (wo == null) return TimeOutcome.rejected("TARGET_NOT_FOUND");
        em.refresh(wo);
        if (!wo.canBeEditedBy(user)) return TimeOutcome.rejected("ACCESS_REVOKED");
        if (manual) {
            if (!hasManualRole(user)) return TimeOutcome.rejected("ACCESS_REVOKED");
            if (!hasManualPlan(user)) return TimeOutcome.rejected("ADDITIONAL_TIME_REQUIRED");
            if (!licenseService.hasEntitlement(LicenseEntitlement.TIME_TRACKING)) return TimeOutcome.rejected("TIME_TRACKING_REQUIRED");
            Labor labor = new Labor(user, user.getRate(), Date.from(startedAt), wo, false, TimeStatus.STOPPED);
            labor.setDuration(seconds);
            if (wo.getFirstTimeToReact() == null) wo.setFirstTimeToReact(Date.from(now));
            return new TimeOutcome(create(labor), null, false, seconds, true);
        }
        Long expectedStatusRevision = statusPredecessor == null ? base.timerStatusRevision() : Long.valueOf(statusPredecessor.revision());
        if ((starting || closeWithoutStart) && (expectedStatusRevision == null
                || expectedStatusRevision != wo.getTimerStatusRevision())) return TimeOutcome.conflict("TIMER_STATUS_CHANGED");

        Long expectedId = base.laborId();
        Long expectedVersion = base.laborVersion();
        UUID expectedRun = time.timerRunId();
        if (acceptedStart != null) {
            var accepted = acceptedStart.timeResult();
            if (accepted == null || accepted.closedWithoutActivation()) return TimeOutcome.conflict("TIMER_ALREADY_STOPPED");
            if (time.laborId() != null && !time.laborId().equals(accepted.laborId())) return TimeOutcome.conflict("LABOR_TARGET_CHANGED");
            if (time.timerRunId() != null && !time.timerRunId().equals(accepted.timerRunId())) return TimeOutcome.conflict("TIMER_RUN_CHANGED");
            expectedId = accepted.laborId();
            expectedVersion = time.laborVersion() == null ? accepted.laborVersion() : time.laborVersion();
            expectedRun = accepted.timerRunId();
            if (!startedAt.equals(accepted.startedAt())) return TimeOutcome.rejected("INVALID_TIME_RANGE");
        } else if (predecessor != null) {
            var accepted = predecessor.timeResult();
            if (predecessor.status() != MobileOfflineResult.Status.APPLIED || accepted == null
                    || predecessor.code() == null || !predecessor.code().startsWith("TIMER_"))
                return TimeOutcome.rejected("INVALID_PREDECESSOR");
            if (expectedId != null && !expectedId.equals(accepted.laborId())) return TimeOutcome.rejected("INVALID_PREDECESSOR");
            expectedId = accepted.laborId();
            expectedVersion = accepted.laborVersion();
        }
        Labor labor;
        if (expectedId == null) {
            if (!starting && !closeWithoutStart) return TimeOutcome.conflict("LABOR_TARGET_CHANGED");
            if (!primary(wo.getId(), user).isEmpty()) return TimeOutcome.conflict("LABOR_TARGET_CHANGED");
            if (!licenseService.hasEntitlement(LicenseEntitlement.TIME_TRACKING)) return TimeOutcome.rejected("TIME_TRACKING_REQUIRED");
            labor = null;
        } else {
            labor = ownedPrimary(expectedId, wo, user);
            if (labor == null) return TimeOutcome.conflict("LABOR_TARGET_CHANGED");
            if (!starting && !closeWithoutStart) {
                if (expectedRun == null || !expectedRun.equals(labor.getTimerRunId())) return TimeOutcome.conflict("TIMER_RUN_CHANGED");
                if (labor.getStatus() != TimeStatus.RUNNING) return TimeOutcome.conflict("TIMER_ALREADY_STOPPED");
            }
            if (expectedVersion == null || expectedVersion != labor.getVersion()) return TimeOutcome.conflict("LABOR_VERSION_CONFLICT");
        }
        if (starting && labor != null && labor.getStatus() == TimeStatus.RUNNING) return TimeOutcome.conflict("TIMER_RUN_CHANGED");
        if (!starting && !closeWithoutStart) {
            if (labor == null || expectedRun == null || !expectedRun.equals(labor.getTimerRunId())) return TimeOutcome.conflict("TIMER_RUN_CHANGED");
            if (labor.getStatus() != TimeStatus.RUNNING) return TimeOutcome.conflict("TIMER_ALREADY_STOPPED");
            if (!startedAt.equals(labor.getStartedAt().toInstant())) return TimeOutcome.conflict("TIMER_RUN_CHANGED");
        }
        long nextDuration;
        try { nextDuration = Math.addExact(labor == null ? 0 : labor.getDuration(), seconds); }
        catch (ArithmeticException invalid) { return TimeOutcome.rejected("INVALID_TIME_RANGE"); }
        if (nextDuration > Integer.MAX_VALUE) return TimeOutcome.rejected("INVALID_TIME_RANGE");

        if (labor == null) {
            labor = new Labor(user, user.getRate(), Date.from(startedAt), wo, true, starting ? TimeStatus.RUNNING : TimeStatus.STOPPED);
            if (starting) labor.setTimerRunId(UUID.randomUUID());
            labor.setDuration(nextDuration);
            labor = create(labor);
        } else {
            if (starting) {
                labor.setStartedAt(Date.from(startedAt)); labor.setStatus(TimeStatus.RUNNING); labor.setTimerRunId(UUID.randomUUID());
            } else {
                labor.setDuration(nextDuration);
                if (!closeWithoutStart) labor.setStatus(TimeStatus.STOPPED);
                // A closed local period never takes over or stops another generation.
            }
            labor = laborRepository.saveAndFlush(labor);
        }
        if (starting || closeWithoutStart) applyStartEffects(wo, startedAt, closeWithoutStart && Boolean.TRUE.equals(time.preserveStatus()));
        return new TimeOutcome(labor, null, false, seconds, closeWithoutStart);
    }
}

