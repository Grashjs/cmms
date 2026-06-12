package com.grash.service;

import com.grash.dto.workOrder.WorkOrderPatchDTO;
import com.grash.dto.workload.*;
import com.grash.exception.CustomException;
import com.grash.mapper.WorkOrderMapper;
import com.grash.model.ShiftConfiguration;
import com.grash.model.ShiftDayConfiguration;
import com.grash.model.ShiftException;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import com.grash.model.enums.Status;
import com.grash.repository.WorkOrderRepository;
import com.grash.utils.Helper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkloadService {

    private final WorkOrderRepository workOrderRepository;
    private final UserService userService;
    private final WorkOrderMapper workOrderMapper;
    private final WorkOrderService workOrderService;

    public WorkloadOverviewDTO getOverview(LocalDate startDate, LocalDate endDate, List<Long> userIds, Long companyId) {
        List<User> users = userIds.stream()
                .map(id -> userService.findByIdAndCompany(id, companyId).orElse(null))
                .filter(Objects::nonNull)
                .toList();

        Date start = Helper.localDateToDate(startDate);
        Date end = Helper.localDateToDate(endDate.plusDays(1));

        Map<Long, Map<LocalDate, List<WorkOrder>>> userWorkOrdersByDate = new HashMap<>();
        for (User user : users) {
            Collection<WorkOrder> userWOs = workOrderRepository
                    .findByUserAndEstimatedStartDateBetween(user.getId(), start, end, companyId);
            Map<LocalDate, List<WorkOrder>> byDate = userWOs.stream()
                    .filter(wo -> wo.getEstimatedStartDate() != null)
                    .collect(Collectors.groupingBy(wo -> Helper.dateToLocalDate(wo.getEstimatedStartDate())));
            userWorkOrdersByDate.put(user.getId(), byDate);
        }

        List<WorkloadDayDTO> days = new ArrayList<>();
        int totalTeamCapacity = 0;
        double totalTeamAllocated = 0;

        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            WorkloadDayDTO dayDTO = new WorkloadDayDTO();
            dayDTO.setDate(current);
            dayDTO.setDayOfWeek(current.getDayOfWeek());

            int dayCapacity = 0;
            double dayAllocated = 0;
            List<WorkloadUserDayDTO> userDays = new ArrayList<>();

            for (User user : users) {
                int capacityMinutes = getUserCapacityForDay(user, current);
                List<WorkOrder> userWOs = userWorkOrdersByDate
                        .getOrDefault(user.getId(), Collections.emptyMap())
                        .getOrDefault(current, Collections.emptyList());

                double allocatedMinutes = userWOs.stream()
                        .mapToDouble(wo -> wo.getEstimatedDuration() * 60)
                        .sum();

                WorkloadUserDayDTO userDay = new WorkloadUserDayDTO();
                userDay.setUserId(user.getId());
                userDay.setFullName(user.getFullName());
                userDay.setCapacityMinutes(capacityMinutes);
                userDay.setAllocatedMinutes(allocatedMinutes);
                userDay.setWorkOrders(userWOs.stream().map(workOrderMapper::toWorkloadDto).toList());

                userDays.add(userDay);
                dayCapacity += capacityMinutes;
                dayAllocated += allocatedMinutes;
            }

            dayDTO.setTeamCapacityMinutes(dayCapacity);
            dayDTO.setTeamAllocatedMinutes(dayAllocated);
            dayDTO.setUsers(userDays);
            days.add(dayDTO);

            totalTeamCapacity += dayCapacity;
            totalTeamAllocated += dayAllocated;

            current = current.plusDays(1);
        }

        WorkloadOverviewDTO overview = new WorkloadOverviewDTO();
        overview.setStartDate(startDate);
        overview.setEndDate(endDate);
        overview.setTeamCapacityMinutes(totalTeamCapacity);
        overview.setTeamAllocatedMinutes(totalTeamAllocated);
        overview.setDays(days);
        return overview;
    }

    public UnscheduledWorkOrdersDTO getUnscheduled(Long companyId, List<Status> statuses) {
        Collection<WorkOrder> unscheduled = workOrderRepository.findUnscheduledByCompany(companyId);

        if (statuses != null && !statuses.isEmpty()) {
            unscheduled = unscheduled.stream()
                    .filter(wo -> statuses.contains(wo.getStatus()))
                    .toList();
        }

        Map<Status, Integer> statusCounts = new HashMap<>();
        for (Status status : Status.values()) {
            if (status == Status.COMPLETE) continue;
            int count = (int) unscheduled.stream().filter(wo -> wo.getStatus() == status).count();
            if (count > 0) {
                statusCounts.put(status, count);
            }
        }

        Date now = new Date();
        Date soon = Helper.addSeconds(now, 2 * 24 * 3600);
        int overdueCount = (int) unscheduled.stream()
                .filter(wo -> wo.getDueDate() != null && wo.getDueDate().before(now))
                .count();
        int dueSoonCount = (int) unscheduled.stream()
                .filter(wo -> wo.getDueDate() != null && !wo.getDueDate().before(now) && wo.getDueDate().before(soon))
                .count();

        UnscheduledWorkOrdersDTO result = new UnscheduledWorkOrdersDTO();
        result.setStatusCounts(statusCounts);
        result.setOverdueCount(overdueCount);
        result.setDueSoonCount(dueSoonCount);
        result.setWorkOrders(unscheduled.stream().map(workOrderMapper::toWorkloadDto).toList());
        return result;
    }

    @Transactional
    public void scheduleWorkOrder(Long workOrderId, WorkloadScheduleDTO dto, User user) {
        WorkOrder workOrder = workOrderRepository.findById(workOrderId)
                .orElseThrow(() -> new CustomException("WorkOrder not found", HttpStatus.NOT_FOUND));

        boolean assigningUser = dto.getLocalDate() != null;
        if (!workOrder.canBeEditedBy(user)) {
            throw new CustomException("Forbidden", HttpStatus.FORBIDDEN);
        }

        if (assigningUser && workOrder.getEstimatedDuration() <= 0) {
            throw new CustomException("Estimated duration must be set and greater than 0 before scheduling",
                    HttpStatus.BAD_REQUEST);
        }
        if (assigningUser && dto.getPrimaryUserId() == null) {
            throw new CustomException("Primary user must be assigned before scheduling",
                    HttpStatus.BAD_REQUEST);
        }

        User assignedUser = null;
        if (dto.getPrimaryUserId() != null) {
            assignedUser = userService.findByIdAndCompany(dto.getPrimaryUserId(), user.getCompany().getId())
                    .orElseThrow(() -> new CustomException("Assigned user not found", HttpStatus.NOT_FOUND));
        }
        workOrder.setPrimaryUser(assignedUser);

        if (assigningUser) {
            int capacityMinutes = getUserCapacityForDay(assignedUser, dto.getLocalDate());

            Date dayStart = Helper.localDateToDate(dto.getLocalDate());
            Date dayEnd = Helper.localDateToDate(dto.getLocalDate().plusDays(1));

            Collection<WorkOrder> existingWOs = workOrderRepository
                    .findByUserAndEstimatedStartDateBetween(assignedUser.getId(), dayStart, dayEnd,
                            user.getCompany().getId());

            double allocatedMinutes = existingWOs.stream()
                    .filter(wo -> !wo.getId().equals(workOrderId))
                    .mapToDouble(wo -> wo.getEstimatedDuration() * 60)
                    .sum();

            double requiredMinutes = workOrder.getEstimatedDuration() * 60;

            if (allocatedMinutes + requiredMinutes > capacityMinutes) {
                throw new CustomException("User is not available on " + dto.getLocalDate()
                        + ". Required: " + String.format("%.0f", requiredMinutes)
                        + " min, Available: " + String.format("%.0f", Math.max(0, capacityMinutes - allocatedMinutes))
                        + " min", HttpStatus.BAD_REQUEST);
            }

            workOrder.setEstimatedStartDate(Helper.localDateToDate(dto.getLocalDate()));
        } else {
            workOrder.setEstimatedStartDate(null);
        }

        workOrderService.save(workOrder);
    }

    public int getUserCapacityForDay(User user, LocalDate date) {
        ShiftConfiguration config = user.getShiftConfiguration();
        if (config == null || !config.isEnabled()) return 0;

        for (ShiftException exception : config.getExceptions()) {
            if (exception.getExceptionDate().equals(date) && exception.isEnabled()) {
                return exception.getAvailabilityMinutes();
            }
        }

        for (ShiftDayConfiguration dayConfig : config.getDays()) {
            if (dayConfig.getDayOfWeek().equals(date.getDayOfWeek()) && dayConfig.isEnabled()) {
                return dayConfig.getAvailabilityMinutes();
            }
        }

        return 0;
    }
}
