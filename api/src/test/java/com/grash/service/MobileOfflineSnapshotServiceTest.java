package com.grash.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grash.dto.offline.MobileOfflineLimits;
import com.grash.exception.CustomException;
import com.grash.mapper.*;
import com.grash.model.*;
import com.grash.model.enums.RoleType;
import com.grash.dto.license.LicenseEntitlement;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class MobileOfflineSnapshotServiceTest {
    EntityManager em = mock(EntityManager.class);
    MobileOfflineAccessService access = mock(MobileOfflineAccessService.class);
    LicenseService license = mock(LicenseService.class);
    MobileOfflineSnapshotService service = new MobileOfflineSnapshotService(em, mock(WorkOrderMapper.class),
            mock(TaskMapper.class), mock(CommentMapper.class), mock(WorkOrderHistoryMapper.class),
            new ObjectMapper().findAndRegisterModules(), access, license);

    @SuppressWarnings("unchecked")
    private void counts(long tasks, long comments, long history, long nested) {
        when(em.createQuery(anyString(), eq(Long.class))).thenAnswer(invocation -> {
            String hql = invocation.getArgument(0);
            long value = hql.startsWith("select count(t)") ? tasks : hql.startsWith("select count(c) from Comment c where")
                    ? comments : hql.startsWith("select count(h)") ? history : nested;
            TypedQuery<Long> query = mock(TypedQuery.class);
            when(query.setParameter(eq("id"), any())).thenReturn(query);
            when(query.getSingleResult()).thenReturn(value);
            return query;
        });
    }

    @Test void perCollectionBoundaryPassesAndOverflowFailsBeforeLoadingEntities() {
        counts(500, 0, 0, 0);
        assertDoesNotThrow(() -> service.verifyCounts(10L));
        counts(501, 0, 0, 0);
        assertEquals(413, assertThrows(CustomException.class, () -> service.verifyCounts(10L)).getHttpStatus().value());
        verify(em, never()).find(any(), any());
    }

    @Test void commentsHistoryAndTotalItemsAreBoundedWithoutTruncation() {
        counts(0, 500, 500, 0); assertDoesNotThrow(() -> service.verifyCounts(10L));
        counts(0, 501, 0, 0); assertThrows(CustomException.class, () -> service.verifyCounts(10L));
        counts(0, 0, 501, 0); assertThrows(CustomException.class, () -> service.verifyCounts(10L));
        counts(500, 499, 500, 0); assertDoesNotThrow(() -> service.verifyCounts(10L)); // Exactly 2,000 items.
        counts(500, 500, 500, 0); assertThrows(CustomException.class, () -> service.verifyCounts(10L));
        counts(0, 0, 0, 2000); assertThrows(CustomException.class, () -> service.verifyCounts(10L));
    }

    @Test void actualUtf8EncodedBoundaryIsEnforcedAndHistoricalTextHasNo255Limit() {
        int max = MobileOfflineLimits.PILOT.snapshotBytes();
        assertDoesNotThrow(() -> service.verifyEncodedSize("a".repeat(max - 2)));
        assertThrows(CustomException.class, () -> service.verifyEncodedSize("a".repeat(max - 1)));
        assertThrows(CustomException.class, () -> service.verifyEncodedSize("é".repeat(max / 2)));
        assertDoesNotThrow(() -> service.verifyEncodedSize(Map.of("historicalComment", "x".repeat(100_000))));
    }

    @Test void accessRemovalIsDistinguishedFromMissingTarget() {
        User user = new User();
        when(access.existsInCompany(10L, user)).thenReturn(false);
        assertEquals(404, assertThrows(CustomException.class, () -> service.build(10, user)).getHttpStatus().value());
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(false);
        assertEquals(403, assertThrows(CustomException.class, () -> service.build(10, user)).getHttpStatus().value());
        verifyNoInteractions(em);
    }

    @Test @SuppressWarnings({"rawtypes", "unchecked"})
    void preparationDistinguishesNewTimerLicenseFromResumeAndStop() {
        counts(0, 0, 0, 0);
        Company company = new Company(); company.setId(52L);
        User user = new User(); user.setId(34L); user.setCompany(company);
        user.setRole(Role.builder().roleType(RoleType.ROLE_CLIENT).viewPermissions(new HashSet<>()).build());
        WorkOrder wo = new WorkOrder(); wo.setId(10L); wo.setCompany(company); wo.setTimerStatusRevision(2);
        when(access.existsInCompany(10L, user)).thenReturn(true);
        when(access.canView(10L, user)).thenReturn(true);
        when(access.canEdit(10L, user)).thenReturn(true);
        when(em.find(WorkOrder.class, 10L)).thenReturn(wo);
        for (Class entity : List.of(Task.class, Comment.class, WorkOrderHistory.class, Labor.class)) {
            TypedQuery query = mock(TypedQuery.class);
            when(em.createQuery(anyString(), eq(entity))).thenReturn(query);
            when(query.setParameter(eq("id"), any())).thenReturn(query);
            when(query.setMaxResults(anyInt())).thenReturn(query);
            when(query.getResultList()).thenReturn(List.of());
        }
        var snapshot = service.build(10L, user);
        assertEquals(2, snapshot.timerStatusRevision());
        assertFalse(snapshot.timePermissions().createTimer());
        assertTrue(snapshot.timePermissions().resumeTimer()); assertTrue(snapshot.timePermissions().stopTimer());
        assertFalse(snapshot.timePermissions().createManualTime());
        when(license.hasEntitlement(LicenseEntitlement.TIME_TRACKING)).thenReturn(true);
        assertTrue(service.build(10L, user).timePermissions().createTimer());
        assertFalse(service.build(10L, user).timePermissions().createManualTime()); // no manual plan
    }

    @Test @SuppressWarnings("unchecked")
    void preparedLaborRowsCountTowardTheExistingSnapshotBound() {
        counts(0, 0, 0, 0);
        TypedQuery<Long> query = mock(TypedQuery.class);
        when(em.createQuery("select count(l) from Labor l where l.workOrder.id = :id", Long.class)).thenReturn(query);
        when(query.setParameter("id", 10L)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(1000L);
        assertEquals("SNAPSHOT_LIMIT_EXCEEDED", assertThrows(CustomException.class, () -> service.verifyCounts(10L)).getMessage());
        verify(em, never()).find(any(), any());
    }
}
