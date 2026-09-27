package com.grash.job;

import com.grash.exception.CustomException;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Schedule;
import com.grash.model.WorkOrder;
import com.grash.repository.ScheduleRepository;
import com.grash.service.PreventiveMaintenanceService;
import com.grash.service.ScheduleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderCreationJobTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private PreventiveMaintenanceService preventiveMaintenanceService;

    @InjectMocks
    private WorkOrderCreationJob workOrderCreationJob;

    private Schedule schedule;

    @BeforeEach
    void setUp() {
        PreventiveMaintenance preventiveMaintenance = new PreventiveMaintenance();
        preventiveMaintenance.setId(1L);
        schedule = new Schedule(preventiveMaintenance);
        schedule.setId(1L);
        schedule.setDisabled(false);
    }

    private JobExecutionContext contextWithScheduleId(long scheduleId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("scheduleId", scheduleId);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
        return context;
    }

    @Test
    void scheduleNotFound_returnsWithoutCreatingWorkOrder() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> workOrderCreationJob.executeInternal(contextWithScheduleId(1L)));

        verify(scheduleService, never()).checkIfWeeklyShouldRun(any());
        verify(preventiveMaintenanceService, never()).createWorkOrderFromPreventiveMaintenance(any());
    }

    @Test
    void disabledSchedule_returnsWithoutCreatingWorkOrder() {
        schedule.setDisabled(true);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertDoesNotThrow(() -> workOrderCreationJob.executeInternal(contextWithScheduleId(1L)));

        verify(scheduleService, never()).checkIfWeeklyShouldRun(any());
        verify(preventiveMaintenanceService, never()).createWorkOrderFromPreventiveMaintenance(any());
    }

    @Test
    void weeklyShouldNotRun_returnsWithoutCreatingWorkOrder() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleService.checkIfWeeklyShouldRun(schedule)).thenReturn(false);

        assertDoesNotThrow(() -> workOrderCreationJob.executeInternal(contextWithScheduleId(1L)));

        verify(preventiveMaintenanceService, never()).createWorkOrderFromPreventiveMaintenance(any());
    }

    @Test
    void validSchedule_createsWorkOrderFromPreventiveMaintenance() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleService.checkIfWeeklyShouldRun(schedule)).thenReturn(true);
        WorkOrder workOrder = new WorkOrder();
        workOrder.setCustomId("WO000042");
        when(preventiveMaintenanceService.createWorkOrderFromPreventiveMaintenance(any()))
                .thenReturn(workOrder);

        assertDoesNotThrow(() -> workOrderCreationJob.executeInternal(contextWithScheduleId(1L)));

        verify(preventiveMaintenanceService).createWorkOrderFromPreventiveMaintenance(schedule.getPreventiveMaintenance());
    }

    @Test
    void workOrderCreationFails_rethrowsSoTheTransactionIsRolledBack() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleService.checkIfWeeklyShouldRun(schedule)).thenReturn(true);
        when(preventiveMaintenanceService.createWorkOrderFromPreventiveMaintenance(any()))
                .thenThrow(new CustomException("You need a license to add a new work order.", HttpStatus.FORBIDDEN));

        assertThrows(CustomException.class, () -> workOrderCreationJob.executeInternal(contextWithScheduleId(1L)));

        verify(preventiveMaintenanceService).createWorkOrderFromPreventiveMaintenance(schedule.getPreventiveMaintenance());
    }

    @Test
    void contextWithoutScheduleId_propagatesError() {
        JobDataMap jobDataMap = new JobDataMap();
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        assertThrows(ClassCastException.class, () -> workOrderCreationJob.executeInternal(context));

        verify(scheduleRepository, never()).findById(anyLong());
        verify(preventiveMaintenanceService, never()).createWorkOrderFromPreventiveMaintenance(any());
    }
}
