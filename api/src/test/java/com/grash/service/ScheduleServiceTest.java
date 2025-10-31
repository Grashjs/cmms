package com.grash.service;

import com.grash.dto.SchedulePatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.ScheduleMapper;
import com.grash.model.*;
import com.grash.model.enums.PermissionEntity;
import com.grash.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private PreventiveMaintenanceService preventiveMaintenanceService;
    @Mock
    private ScheduleMapper scheduleMapper;
    @Mock
    private MessageSource messageSource;
    @Mock
    private EmailService2 emailService2;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private TaskService taskService;
    @Mock
    private UserService userService;

    @InjectMocks
    private ScheduleService scheduleService;

    private Schedule schedule;

    @BeforeEach
    void setUp() {
        schedule = new Schedule();
        schedule.setId(1L);
        schedule.setFrequency(30);
        schedule.setStartsOn(new Date());

        Company company = new Company();
        company.setId(1L);
        CompanySettings companySettings = new CompanySettings();
        GeneralPreferences generalPreferences = new GeneralPreferences();
        generalPreferences.setDaysBeforePrevMaintNotification(5);
        companySettings.setGeneralPreferences(generalPreferences);
        company.setCompanySettings(companySettings);

        PreventiveMaintenance pm = new PreventiveMaintenance();
        pm.setId(1L);
        pm.setCompany(company);
        schedule.setPreventiveMaintenance(pm);
    }

    @Nested
    @DisplayName("CRUD Operations")
    class CrudTests {

        @Test
        @DisplayName("should create a schedule")
        void create() {
            when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);
            Schedule result = scheduleService.create(schedule);
            assertNotNull(result);
            assertEquals(schedule.getId(), result.getId());
            verify(scheduleRepository).save(schedule);
        }

        @Test
        @DisplayName("should update a schedule")
        void update() {
            SchedulePatchDTO patchDTO = new SchedulePatchDTO();
            when(scheduleRepository.existsById(1L)).thenReturn(true);
            when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
            when(scheduleMapper.updateSchedule(any(Schedule.class), any(SchedulePatchDTO.class))).thenReturn(schedule);
            when(scheduleRepository.save(any(Schedule.class))).thenReturn(schedule);

            Schedule result = scheduleService.update(1L, patchDTO);

            assertNotNull(result);
            verify(scheduleRepository).save(schedule);
        }

        @Test
        @DisplayName("should throw exception when updating non-existent schedule")
        void updateNotFound() {
            SchedulePatchDTO patchDTO = new SchedulePatchDTO();
            when(scheduleRepository.existsById(1L)).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> {
                scheduleService.update(1L, patchDTO);
            });

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
        }

        @Test
        @DisplayName("should get all schedules")
        void getAll() {
            when(scheduleRepository.findAll()).thenReturn(Collections.singletonList(schedule));
            Collection<Schedule> result = scheduleService.getAll();
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(scheduleRepository).findAll();
        }

        @Test
        @DisplayName("should delete a schedule")
        void deleteSchedule() {
            doNothing().when(scheduleRepository).deleteById(1L);
            scheduleService.delete(1L);
            verify(scheduleRepository).deleteById(1L);
        }

        @Test
        @DisplayName("should find a schedule by id")
        void findById() {
            when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
            Optional<Schedule> result = scheduleService.findById(1L);
            assertTrue(result.isPresent());
            assertEquals(schedule.getId(), result.get().getId());
            verify(scheduleRepository).findById(1L);
        }

        @Test
        @DisplayName("should find schedules by company")
        void findByCompany() {
            when(scheduleRepository.findByCompany_Id(1L)).thenReturn(Collections.singletonList(schedule));
            Collection<Schedule> result = scheduleService.findByCompany(1L);
            assertFalse(result.isEmpty());
            assertEquals(1, result.size());
            verify(scheduleRepository).findByCompany_Id(1L);
        }

        @Test
        @DisplayName("should save a schedule")
        void save() {
            when(scheduleRepository.saveAndFlush(any(Schedule.class))).thenReturn(schedule);
            Schedule result = scheduleService.save(schedule);
            assertNotNull(result);
            verify(scheduleRepository).saveAndFlush(schedule);
        }
    }

    @Nested
    @DisplayName("Scheduling Logic")
    class SchedulingTests {

        @BeforeEach
        void cleanUpTimers() throws NoSuchFieldException, IllegalAccessException {
            // Clean up timers state before each test to ensure isolation
            Field timersStateField = ScheduleService.class.getDeclaredField("timersState");
            timersStateField.setAccessible(true);
            Map<Long, Map<String, Timer>> timersState = (Map<Long, Map<String, Timer>>) timersStateField.get(scheduleService);
            timersState.clear();
        }

        @Test
        @DisplayName("should disable schedule if it becomes stale")
        void scheduleWorkOrder_stale() {
            List<WorkOrder> workOrders = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                workOrders.add(new WorkOrder());
            }
            Page<WorkOrder> workOrderPage = new PageImpl<>(workOrders);
            when(workOrderService.findLastByPM(anyLong(), anyInt())).thenReturn(workOrderPage);

            scheduleService.scheduleWorkOrder(schedule);

            assertTrue(schedule.isDisabled());
            verify(scheduleRepository).save(schedule);
        }

        @Test
        @DisplayName("should not schedule if disabled")
        void scheduleWorkOrder_disabled() {
            schedule.setDisabled(true);
            Page<WorkOrder> emptyPage = new PageImpl<>(Collections.emptyList());
            when(workOrderService.findLastByPM(anyLong(), anyInt())).thenReturn(emptyPage);

            scheduleService.scheduleWorkOrder(schedule);

            verify(scheduleRepository, never()).save(any(Schedule.class));
        }

        @Test
        @DisplayName("should not schedule if endsOn is in the past")
        void scheduleWorkOrder_ended() {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, -1);
            schedule.setEndsOn(cal.getTime());
            Page<WorkOrder> emptyPage = new PageImpl<>(Collections.emptyList());
            when(workOrderService.findLastByPM(anyLong(), anyInt())).thenReturn(emptyPage);

            scheduleService.scheduleWorkOrder(schedule);

            verify(scheduleRepository, never()).save(any(Schedule.class));
        }

        @Test
        @DisplayName("should schedule work order with notification and stop timers")
        void scheduleWorkOrder_withNotificationAndStop() throws NoSuchFieldException, IllegalAccessException {
            // Arrange
            Page<WorkOrder> emptyPage = new PageImpl<>(Collections.emptyList());
            when(workOrderService.findLastByPM(anyLong(), anyInt())).thenReturn(emptyPage);

            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DATE, 30);
            schedule.setEndsOn(cal.getTime());

            // Act
            scheduleService.scheduleWorkOrder(schedule);

            // Assert
            Field timersStateField = ScheduleService.class.getDeclaredField("timersState");
            timersStateField.setAccessible(true);
            Map<Long, Map<String, Timer>> timersState = (Map<Long, Map<String, Timer>>) timersStateField.get(scheduleService);

            assertTrue(timersState.containsKey(1L));
            Map<String, Timer> localTimers = timersState.get(1L);
            assertTrue(localTimers.containsKey("wo_creation"));
            assertTrue(localTimers.containsKey("notification"));
            assertTrue(localTimers.containsKey("stop"));

            // Clean up timers
            scheduleService.stopScheduleTimers(1L);
        }

        @Test
        @DisplayName("should schedule work order without notification timer")
        void scheduleWorkOrder_withoutNotification() throws NoSuchFieldException, IllegalAccessException {
            // Arrange
            schedule.getPreventiveMaintenance().getCompany().getCompanySettings().getGeneralPreferences().setDaysBeforePrevMaintNotification(0);
            Page<WorkOrder> emptyPage = new PageImpl<>(Collections.emptyList());
            when(workOrderService.findLastByPM(anyLong(), anyInt())).thenReturn(emptyPage);

            // Act
            scheduleService.scheduleWorkOrder(schedule);

            // Assert
            Field timersStateField = ScheduleService.class.getDeclaredField("timersState");
            timersStateField.setAccessible(true);
            Map<Long, Map<String, Timer>> timersState = (Map<Long, Map<String, Timer>>) timersStateField.get(scheduleService);

            assertTrue(timersState.containsKey(1L));
            Map<String, Timer> localTimers = timersState.get(1L);
            assertTrue(localTimers.containsKey("wo_creation"));
            assertFalse(localTimers.containsKey("notification"));
            assertFalse(localTimers.containsKey("stop"));

            // Clean up timers
            scheduleService.stopScheduleTimers(1L);
        }

        @Test
        @DisplayName("should stop schedule timers")
        void stopScheduleTimers() throws NoSuchFieldException, IllegalAccessException {
            // Use reflection to access the private timersState map
            Field timersStateField = ScheduleService.class.getDeclaredField("timersState");
            timersStateField.setAccessible(true);
            Map<Long, Map<String, Timer>> timersState = (Map<Long, Map<String, Timer>>) timersStateField.get(scheduleService);

            // Prepare mock timers
            Timer woCreationTimer = mock(Timer.class);
            Timer notificationTimer = mock(Timer.class);
            Map<String, Timer> localTimers = new HashMap<>();
            localTimers.put("wo_creation", woCreationTimer);
            localTimers.put("notification", notificationTimer);
            timersState.put(1L, localTimers);

            // Call the method to test
            scheduleService.stopScheduleTimers(1L);

            // Verify timers are cancelled and purged
            verify(woCreationTimer).cancel();
            verify(woCreationTimer).purge();
            verify(notificationTimer).cancel();
            verify(notificationTimer).purge();
        }

        @Test
        @DisplayName("should not throw when stopping non-existent timers")
        void stopScheduleTimers_nonExistent() {
            assertDoesNotThrow(() -> scheduleService.stopScheduleTimers(99L));
        }

        @Test
        @DisplayName("should reschedule work order")
        void reScheduleWorkOrder() {
            // We can't verify the timer interactions directly, so we spy on the service
            ScheduleService spyService = spy(scheduleService);
            doNothing().when(spyService).stopScheduleTimers(anyLong());
            doNothing().when(spyService).scheduleWorkOrder(any(Schedule.class));

            spyService.reScheduleWorkOrder(1L, schedule);

            verify(spyService).stopScheduleTimers(1L);
            verify(spyService).scheduleWorkOrder(schedule);
        }
    }
}
