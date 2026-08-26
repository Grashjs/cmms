package com.grash.job;

import com.grash.factory.MailServiceFactory;
import com.grash.model.Company;
import com.grash.model.CompanySettings;
import com.grash.model.GeneralPreferences;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.Role;
import com.grash.model.Schedule;
import com.grash.model.Subscription;
import com.grash.model.User;
import com.grash.model.UserSettings;
import com.grash.model.enums.PermissionEntity;
import com.grash.repository.ScheduleRepository;
import com.grash.service.MailService;
import com.grash.service.ScheduleService;
import com.grash.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.context.MessageSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PreventiveMaintenanceNotificationJobTest {

    @Mock
    private ScheduleRepository scheduleRepository;
    @Mock
    private UserService userService;
    @Mock
    private MailServiceFactory mailServiceFactory;
    @Mock
    private MessageSource messageSource;
    @Mock
    private ScheduleService scheduleService;
    @Mock
    private MailService mailService;

    @InjectMocks
    private PreventiveMaintenanceNotificationJob preventiveMaintenanceNotificationJob;

    private Company company;
    private Schedule schedule;
    private PreventiveMaintenance preventiveMaintenance;
    private Role adminRole;
    private Role workerRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(preventiveMaintenanceNotificationJob, "frontendUrl", "http://localhost:3000");

        CompanySettings companySettings = new CompanySettings();
        companySettings.setId(1L);
        GeneralPreferences generalPreferences = new GeneralPreferences(companySettings);
        companySettings.setGeneralPreferences(generalPreferences);
        company = new Company("TestCo", 10, Subscription.builder().id(1L).build());
        company.setId(1L);
        company.setCompanySettings(companySettings);

        preventiveMaintenance = new PreventiveMaintenance();
        preventiveMaintenance.setId(1L);
        preventiveMaintenance.setTitle("Engine Check");
        preventiveMaintenance.setCompany(company);
        preventiveMaintenance.setAssignedTo(new ArrayList<>());

        schedule = new Schedule(preventiveMaintenance);
        schedule.setId(1L);
        schedule.setDisabled(false);

        adminRole = Role.builder()
                .id(1L)
                .name("Admin")
                .viewPermissions(new HashSet<>(Collections.singletonList(PermissionEntity.SETTINGS)))
                .build();
        workerRole = Role.builder()
                .id(2L)
                .name("Worker")
                .viewPermissions(new HashSet<>())
                .build();
    }

    private JobExecutionContext contextWithScheduleId(long scheduleId) {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("scheduleId", scheduleId);
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);
        return context;
    }

    private User buildUser(Long id, Role role, boolean enabled, boolean emailUpdatesForWorkOrders) {
        User user = new User();
        user.setId(id);
        user.setFirstName("U" + id);
        user.setLastName("L" + id);
        user.setEmail("u" + id + "@test.com");
        user.setRole(role);
        user.setCompany(company);
        user.setEnabled(enabled);
        UserSettings userSettings = new UserSettings();
        userSettings.setEmailUpdatesForWorkOrders(emailUpdatesForWorkOrders);
        user.setUserSettings(userSettings);
        return user;
    }

    private void stubMailDefaults() {
        when(mailServiceFactory.getMailService()).thenReturn(mailService);
        when(messageSource.getMessage(eq("coming_wo"), isNull(), any())).thenReturn("Upcoming WO");
    }

    @Test
    void scheduleNotFound_returnsWithoutSendingMail() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> preventiveMaintenanceNotificationJob.executeInternal(contextWithScheduleId(1L)));

        verify(mailServiceFactory, never()).getMailService();
        verify(mailService, never()).sendMessageUsingThymeleafTemplate(
                any(String[].class), anyString(), anyMap(), anyString(), any());
    }

    @Test
    void disabledSchedule_returnsWithoutSendingMail() {
        schedule.setDisabled(true);
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));

        assertDoesNotThrow(() -> preventiveMaintenanceNotificationJob.executeInternal(contextWithScheduleId(1L)));

        verify(mailServiceFactory, never()).getMailService();
    }

    @Test
    void weeklyShouldNotRun_returnsWithoutSendingMail() {
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleService.checkIfWeeklyShouldRun(schedule)).thenReturn(false);

        assertDoesNotThrow(() -> preventiveMaintenanceNotificationJob.executeInternal(contextWithScheduleId(1L)));

        verify(mailServiceFactory, never()).getMailService();
    }

    @Test
    void sendsToPmUsersAndAdmins_deduplicatedById() {
        User pmUser = buildUser(1L, workerRole, true, true);
        User adminOnly = buildUser(2L, adminRole, true, true);
        preventiveMaintenance.setAssignedTo(new ArrayList<>(List.of(pmUser)));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleService.checkIfWeeklyShouldRun(schedule)).thenReturn(true);
        when(userService.findWorkersByCompany(1L)).thenReturn(List.of(pmUser, adminOnly));
        stubMailDefaults();

        assertDoesNotThrow(() -> preventiveMaintenanceNotificationJob.executeInternal(contextWithScheduleId(1L)));

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mailService).sendMessageUsingThymeleafTemplate(
                captor.capture(), eq("Upcoming WO"), anyMap(), eq("coming-work-order.html"), any());
        assertEquals(Set.of("u1@test.com", "u2@test.com"), new HashSet<>(List.of(captor.getValue())));
    }

    @Test
    void filtersOutDisabledUsersAndUsersOptedOut() {
        User optedOut = buildUser(1L, workerRole, true, false);
        User disabled = buildUser(2L, workerRole, false, true);
        User eligible = buildUser(3L, workerRole, true, true);
        preventiveMaintenance.setAssignedTo(new ArrayList<>(List.of(optedOut, disabled, eligible)));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleService.checkIfWeeklyShouldRun(schedule)).thenReturn(true);
        when(userService.findWorkersByCompany(1L)).thenReturn(Collections.emptyList());
        stubMailDefaults();

        assertDoesNotThrow(() -> preventiveMaintenanceNotificationJob.executeInternal(contextWithScheduleId(1L)));

        ArgumentCaptor<String[]> captor = ArgumentCaptor.forClass(String[].class);
        verify(mailService).sendMessageUsingThymeleafTemplate(
                captor.capture(), eq("Upcoming WO"), anyMap(), eq("coming-work-order.html"), any());
        assertEquals(1, captor.getValue().length);
        assertEquals("u3@test.com", captor.getValue()[0]);
    }

    @Test
    void mailVariablesContainPmLinkAndTitle() {
        User eligible = buildUser(1L, workerRole, true, true);
        preventiveMaintenance.setAssignedTo(new ArrayList<>(List.of(eligible)));
        when(scheduleRepository.findById(1L)).thenReturn(Optional.of(schedule));
        when(scheduleService.checkIfWeeklyShouldRun(schedule)).thenReturn(true);
        when(userService.findWorkersByCompany(1L)).thenReturn(Collections.emptyList());
        stubMailDefaults();

        assertDoesNotThrow(() -> preventiveMaintenanceNotificationJob.executeInternal(contextWithScheduleId(1L)));

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(mailService).sendMessageUsingThymeleafTemplate(
                any(String[].class), eq("Upcoming WO"), variablesCaptor.capture(), eq("coming-work-order.html"),
                eq(Locale.getDefault()));
        assertEquals("http://localhost:3000/app/preventive-maintenances/1",
                variablesCaptor.getValue().get("pmLink"));
        assertEquals("Engine Check", variablesCaptor.getValue().get("pmTitle"));
    }

    @Test
    void contextWithoutScheduleId_propagatesError() {
        JobDataMap jobDataMap = new JobDataMap();
        JobExecutionContext context = mock(JobExecutionContext.class);
        when(context.getMergedJobDataMap()).thenReturn(jobDataMap);

        assertThrows(ClassCastException.class,
                () -> preventiveMaintenanceNotificationJob.executeInternal(context));

        verify(scheduleRepository, never()).findById(any());
        verify(mailService, never()).sendMessageUsingThymeleafTemplate(
                any(String[].class), anyString(), anyMap(), anyString(), any());
    }
}
