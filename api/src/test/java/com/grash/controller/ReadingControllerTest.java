package com.grash.controller;

import com.grash.dto.ReadingPatchDTO;
import com.grash.dto.SuccessResponse;
import com.grash.exception.CustomException;
import com.grash.model.*;
import com.grash.model.enums.NotificationType;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import com.grash.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReadingControllerTest {

    @Mock
    private MeterService meterService;
    @Mock
    private ReadingService readingService;
    @Mock
    private UserService userService;
    @Mock
    private WorkOrderMeterTriggerService workOrderMeterTriggerService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private WorkOrderService workOrderService;
    @Mock
    private MessageSource messageSource;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private ReadingController readingController;

    private OwnUser user;
    private Meter meter;
    private Reading reading;

    @BeforeEach
    void setUp() {
        user = new OwnUser();
        user.setId(1L);
        user.setCompany(new Company());
        

        meter = new Meter();
        meter.setId(1L);
        meter.setUsers(Collections.singletonList(user));

        reading = new Reading();
        reading.setId(1L);
        reading.setMeter(meter);
    }

    @Nested
    @DisplayName("Get By Meter Tests")
    class GetByMeterTests {

        @Test
        @DisplayName("Should throw not found when meter does not exist")
        void shouldThrowNotFoundWhenMeterDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> readingController.getByMeter(1L, request));
        }

        @Test
        @DisplayName("Should return readings for meter")
        void shouldReturnReadingsForMeter() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(1L)).thenReturn(Optional.of(meter));
            when(readingService.findByMeter(1L)).thenReturn(Collections.singletonList(reading));

            Collection<Reading> result = readingController.getByMeter(1L, request);

            assertFalse(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("Create Tests")
    class CreateTests {

        @Test
        @DisplayName("Should throw not found when meter does not exist")
        void shouldThrowNotFoundWhenMeterDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(anyLong())).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> readingController.create(reading, request));
        }

        @Test
        @DisplayName("Should create reading and trigger notifications")
        void shouldCreateReadingAndTriggerNotifications() {
            WorkOrderMeterTrigger trigger = new WorkOrderMeterTrigger();
            trigger.setTriggerCondition(WorkOrderMeterTriggerCondition.LESS_THAN);
            trigger.setValue(100);

            reading.setValue(50);

            when(userService.whoami(request)).thenReturn(user);
            when(meterService.findById(anyLong())).thenReturn(Optional.of(meter));
            when(readingService.findByMeter(anyLong())).thenReturn(Collections.emptyList());
            when(workOrderMeterTriggerService.findByMeter(anyLong())).thenReturn(Collections.singletonList(trigger));
            when(readingService.create(reading)).thenReturn(reading);
            when(workOrderService.getWorkOrderFromWorkOrderBase(any(WorkOrderMeterTrigger.class))).thenReturn(new WorkOrder());
            when(messageSource.getMessage(any(), any(), any())).thenReturn("test message");

            Reading result = readingController.create(reading, request);

            assertNotNull(result);
            verify(notificationService).createMultiple(any(), any(Boolean.class), any());
            verify(workOrderService).create(any(), any());
        }
    }

    @Nested
    @DisplayName("Patch Tests")
    class PatchTests {

        private ReadingPatchDTO patchDTO;

        @BeforeEach
        void setup() {
            patchDTO = new ReadingPatchDTO();
        }

        @Test
        @DisplayName("Should throw not found when reading does not exist")
        void shouldThrowNotFoundWhenReadingDoesNotExist() {
            when(userService.whoami(request)).thenReturn(user);
            when(readingService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> readingController.patch(patchDTO, 1L, request));
        }

        @Test
        @DisplayName("Should patch reading successfully")
        void shouldPatchReadingSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(readingService.findById(1L)).thenReturn(Optional.of(reading));
            when(readingService.update(1L, patchDTO)).thenReturn(reading);

            Reading result = readingController.patch(patchDTO, 1L, request);

            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Delete Tests")
    class DeleteTests {

        @Test
        @DisplayName("Should throw not found on delete")
        void shouldThrowNotFoundOnDelete() {
            when(userService.whoami(request)).thenReturn(user);
            when(readingService.findById(1L)).thenReturn(Optional.empty());

            assertThrows(CustomException.class, () -> readingController.delete(1L, request));
        }

        @Test
        @DisplayName("Should delete reading successfully")
        void shouldDeleteReadingSuccessfully() {
            when(userService.whoami(request)).thenReturn(user);
            when(readingService.findById(1L)).thenReturn(Optional.of(reading));

            ResponseEntity<SuccessResponse> response = readingController.delete(1L, request);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(readingService).delete(1L);
        }
    }
}
