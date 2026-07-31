package com.grash.utils;

import com.grash.aspect.TenantAspect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TenantAspectUtilsTest {

    @Test
    void supplier_returnsResult() {
        try (MockedStatic<TenantAspect> tenantAspect = mockStatic(TenantAspect.class)) {
            String result = TenantAspectUtils.executeWithDisabledCompanyCheck(() -> "done");
            assertEquals("done", result);
        }
    }

    @Test
    void supplier_disablesThenEnablesCompanyCheck() {
        try (MockedStatic<TenantAspect> tenantAspect = mockStatic(TenantAspect.class)) {
            TenantAspectUtils.executeWithDisabledCompanyCheck(() -> null);
            tenantAspect.verify(TenantAspect::disableCompanyCheck);
            tenantAspect.verify(TenantAspect::enableCompanyCheck);
        }
    }

    @Test
    void supplier_throws_stillEnablesCompanyCheck() {
        try (MockedStatic<TenantAspect> tenantAspect = mockStatic(TenantAspect.class)) {
            assertThrows(IllegalStateException.class, () -> TenantAspectUtils.executeWithDisabledCompanyCheck(() -> {
                throw new IllegalStateException("boom");
            }));
            tenantAspect.verify(TenantAspect::disableCompanyCheck);
            tenantAspect.verify(TenantAspect::enableCompanyCheck);
        }
    }

    @Test
    void runnable_runsAndReenables() {
        try (MockedStatic<TenantAspect> tenantAspect = mockStatic(TenantAspect.class)) {
            AtomicBoolean ran = new AtomicBoolean(false);
            TenantAspectUtils.executeWithDisabledCompanyCheck(() -> ran.set(true));
            assertTrue(ran.get());
            tenantAspect.verify(TenantAspect::disableCompanyCheck);
            tenantAspect.verify(TenantAspect::enableCompanyCheck);
        }
    }

    @Test
    void runnable_throws_stillEnablesCompanyCheck() {
        try (MockedStatic<TenantAspect> tenantAspect = mockStatic(TenantAspect.class)) {
            assertThrows(IllegalStateException.class, () -> TenantAspectUtils.executeWithDisabledCompanyCheck(() -> {
                throw new IllegalStateException("boom");
            }));
            tenantAspect.verify(TenantAspect::disableCompanyCheck);
            tenantAspect.verify(TenantAspect::enableCompanyCheck);
        }
    }
}
