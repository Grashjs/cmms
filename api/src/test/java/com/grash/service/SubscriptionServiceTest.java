package com.grash.service;

import com.grash.dto.SubscriptionPatchDTO;
import com.grash.exception.CustomException;
import com.grash.mapper.SubscriptionMapper;
import com.grash.model.Subscription;
import com.grash.model.SubscriptionPlan;
import com.grash.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction; // Added import
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import javax.persistence.EntityManager;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private CompanyService companyService;

    @Mock
    private SubscriptionPlanService subscriptionPlanService;

    @Mock
    private SubscriptionMapper subscriptionMapper;

    @Mock
    private EntityManager em;

    private Subscription subscription;
    private SubscriptionPatchDTO subscriptionPatchDTO;
    private SubscriptionPlan freePlan;
    private SubscriptionPlan paidPlan;

    @BeforeEach
    void setUp() {
        freePlan = new SubscriptionPlan();
        freePlan.setId(1L);
        freePlan.setCode("FREE");

        paidPlan = new SubscriptionPlan();
        paidPlan.setId(2L);
        paidPlan.setCode("PAID");

        subscription = new Subscription();
        subscription.setId(1L);
        subscription.setSubscriptionPlan(paidPlan);
        subscription.setEndsOn(new Date(System.currentTimeMillis() + 100000)); // 100 seconds from now

        subscriptionPatchDTO = new SubscriptionPatchDTO();
        // subscriptionPatchDTO.setActivated(true); // Removed as SubscriptionPatchDTO does not have 'activated' field
    }

    @Nested
    @DisplayName("create method")
    class Create {
        @Test
        @DisplayName("should create and return a new subscription")
        void shouldCreateAndReturnNewSubscription() {
            when(subscriptionRepository.saveAndFlush(any(Subscription.class))).thenReturn(subscription);
            doNothing().when(em).refresh(any(Subscription.class));

            Subscription createdSubscription = subscriptionService.create(subscription);

            assertNotNull(createdSubscription);
            assertEquals(subscription.getId(), createdSubscription.getId());
            verify(subscriptionRepository, times(1)).saveAndFlush(any(Subscription.class));
            verify(em, times(1)).refresh(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("update method")
    class Update {
        @Test
        @DisplayName("should update an existing subscription")
        void shouldUpdateExistingSubscription() {
            when(subscriptionRepository.existsById(anyLong())).thenReturn(true);
            when(subscriptionRepository.findById(anyLong())).thenReturn(Optional.of(subscription));
            when(subscriptionMapper.updateSubscription(any(Subscription.class), any(SubscriptionPatchDTO.class))).thenReturn(subscription);
            when(subscriptionRepository.saveAndFlush(any(Subscription.class))).thenReturn(subscription);
            doNothing().when(em).refresh(any(Subscription.class));

            Subscription updatedSubscription = subscriptionService.update(1L, subscriptionPatchDTO);

            assertNotNull(updatedSubscription);
            assertEquals(subscription.getId(), updatedSubscription.getId());
            verify(subscriptionRepository, times(1)).existsById(1L);
            verify(subscriptionRepository, times(1)).findById(1L);
            verify(subscriptionMapper, times(1)).updateSubscription(subscription, subscriptionPatchDTO);
            verify(subscriptionRepository, times(1)).saveAndFlush(subscription);
            verify(em, times(1)).refresh(subscription);
        }

        @Test
        @DisplayName("should throw CustomException when subscription not found")
        void shouldThrowCustomExceptionWhenSubscriptionNotFound() {
            when(subscriptionRepository.existsById(anyLong())).thenReturn(false);

            CustomException exception = assertThrows(CustomException.class, () -> {
                subscriptionService.update(1L, subscriptionPatchDTO);
            });

            assertEquals("Not found", exception.getMessage());
            assertEquals(HttpStatus.NOT_FOUND, exception.getHttpStatus());
            verify(subscriptionRepository, times(1)).existsById(1L);
            verify(subscriptionRepository, never()).findById(anyLong());
            verify(subscriptionMapper, never()).updateSubscription(any(Subscription.class), any(SubscriptionPatchDTO.class));
            verify(subscriptionRepository, never()).saveAndFlush(any(Subscription.class));
            verify(em, never()).refresh(any(Subscription.class));
        }
    }

    @Nested
    @DisplayName("save method")
    class Save {
        @Test
        @DisplayName("should save a subscription")
        void shouldSaveSubscription() {
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

            subscriptionService.save(subscription);

            verify(subscriptionRepository, times(1)).save(subscription);
        }
    }

    @Nested
    @DisplayName("getAll method")
    class GetAll {
        @Test
        @DisplayName("should return a collection of all subscriptions")
        void shouldReturnCollectionOfAllSubscriptions() {
            List<Subscription> subscriptions = Arrays.asList(subscription, new Subscription());
            when(subscriptionRepository.findAll()).thenReturn(subscriptions);

            Collection<Subscription> result = subscriptionService.getAll();

            assertNotNull(result);
            assertEquals(2, result.size());
            verify(subscriptionRepository, times(1)).findAll();
        }

        @Test
        @DisplayName("should return an empty collection when no subscriptions exist")
        void shouldReturnEmptyCollectionWhenNoSubscriptionsExist() {
            when(subscriptionRepository.findAll()).thenReturn(Collections.emptyList());

            Collection<Subscription> result = subscriptionService.getAll();

            assertNotNull(result);
            assertTrue(result.isEmpty());
            verify(subscriptionRepository, times(1)).findAll();
        }
    }

    @Nested
    @DisplayName("delete method")
    class Delete {
        @Test
        @DisplayName("should delete a subscription by id")
        void shouldDeleteSubscriptionById() {
            doNothing().when(subscriptionRepository).deleteById(anyLong());

            subscriptionService.delete(1L);

            verify(subscriptionRepository, times(1)).deleteById(1L);
        }
    }

    @Nested
    @DisplayName("findById method")
    class FindById {
        @Test
        @DisplayName("should return an Optional with subscription if found")
        void shouldReturnOptionalWithSubscriptionIfFound() {
            when(subscriptionRepository.findById(anyLong())).thenReturn(Optional.of(subscription));

            Optional<Subscription> result = subscriptionService.findById(1L);

            assertTrue(result.isPresent());
            assertEquals(subscription.getId(), result.get().getId());
            verify(subscriptionRepository, times(1)).findById(1L);
        }

        @Test
        @DisplayName("should return an empty Optional if subscription not found")
        void shouldReturnEmptyOptionalIfSubscriptionNotFound() {
            when(subscriptionRepository.findById(anyLong())).thenReturn(Optional.empty());

            Optional<Subscription> result = subscriptionService.findById(1L);

            assertFalse(result.isPresent());
            verify(subscriptionRepository, times(1)).findById(1L);
        }
    }

    @Nested
    @DisplayName("scheduleEnd method")
    class ScheduleEnd {
        @Test
        @DisplayName("should schedule a task when conditions are met")
        void shouldScheduleTaskWhenConditionsAreMet() {
            try (MockedConstruction<Timer> mockedConstruction = mockConstruction(Timer.class)) {
                subscriptionService.scheduleEnd(subscription);
                Timer mockTimer = mockedConstruction.constructed().get(0);

                verify(mockTimer, times(1)).schedule(any(TimerTask.class), any(Date.class));
            }
        }

        @Test
        @DisplayName("should not schedule a task for FREE plan")
        void shouldNotScheduleTaskForFreePlan() {
            subscription.setSubscriptionPlan(freePlan);

            try (MockedConstruction<Timer> mockedConstruction = mockConstruction(Timer.class)) {
                subscriptionService.scheduleEnd(subscription);
                // No Timer should be constructed, so constructed() should be empty
                assertTrue(mockedConstruction.constructed().isEmpty());
            }
        }

        @Test
        @DisplayName("should not schedule a task when endsOn is null")
        void shouldNotScheduleTaskWhenEndsOnIsNull() {
            subscription.setEndsOn(null);

            try (MockedConstruction<Timer> mockedConstruction = mockConstruction(Timer.class)) {
                subscriptionService.scheduleEnd(subscription);
                // No Timer should be constructed, so constructed() should be empty
                assertTrue(mockedConstruction.constructed().isEmpty());
            }
        }
    }

    @Nested
    @DisplayName("findByFastSpringId method")
    class FindByFastSpringId {
        @Test
        @DisplayName("should return an Optional with subscription if found")
        void shouldReturnOptionalWithSubscriptionIfFound() {
            when(subscriptionRepository.findByFastSpringId(anyString())).thenReturn(Optional.of(subscription));

            Optional<Subscription> result = subscriptionService.findByFastSpringId("fastspring-id");

            assertTrue(result.isPresent());
            assertEquals(subscription.getId(), result.get().getId());
            verify(subscriptionRepository, times(1)).findByFastSpringId("fastspring-id");
        }

        @Test
        @DisplayName("should return an empty Optional if subscription not found")
        void shouldReturnEmptyOptionalIfSubscriptionNotFound() {
            when(subscriptionRepository.findByFastSpringId(anyString())).thenReturn(Optional.empty());

            Optional<Subscription> result = subscriptionService.findByFastSpringId("fastspring-id");

            assertFalse(result.isPresent());
            verify(subscriptionRepository, times(1)).findByFastSpringId("fastspring-id");
        }
    }

    @Nested
    @DisplayName("resetToFreePlan method")
    class ResetToFreePlan {
        @Test
        @DisplayName("should reset subscription to free plan")
        void shouldResetSubscriptionToFreePlan() {
            when(subscriptionPlanService.findByCode("FREE")).thenReturn(Optional.of(freePlan));
            when(subscriptionRepository.save(any(Subscription.class))).thenReturn(subscription);

            subscriptionService.resetToFreePlan(subscription);

            assertFalse(subscription.isActivated()); // Corrected getter
            assertEquals(3, subscription.getUsersCount());
            assertTrue(subscription.isMonthly()); // Corrected getter
            assertNull(subscription.getFastSpringId());
            assertFalse(subscription.isCancelled()); // Corrected getter
            assertEquals(freePlan, subscription.getSubscriptionPlan());
            assertNotNull(subscription.getStartsOn());
            assertNull(subscription.getEndsOn());
            verify(subscriptionPlanService, times(1)).findByCode("FREE");
            verify(subscriptionRepository, times(1)).save(subscription);
        }
    }
}