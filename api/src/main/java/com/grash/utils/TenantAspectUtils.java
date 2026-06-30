package com.grash.utils;

import com.grash.aspect.TenantAspect;

import java.util.function.Supplier;

public class TenantAspectUtils {

    /**
     * Executes a supplier function with company check disabled, ensuring it's re-enabled afterwards.
     *
     * @param supplier The function to execute with disabled company check
     * @param <T>      The return type
     * @return The result of the supplier function
     */
    public static <T> T executeWithDisabledCompanyCheck(Supplier<T> supplier) {
        TenantAspect.disableCompanyCheck();
        try {
            return supplier.get();
        } finally {
            TenantAspect.enableCompanyCheck();
        }
    }

    /**
     * Executes a runnable with company check disabled, ensuring it's re-enabled afterwards.
     * For operations that don't return a value.
     *
     * @param runnable The operation to execute with disabled company check
     */
    public static void executeWithDisabledCompanyCheck(Runnable runnable) {
        TenantAspect.disableCompanyCheck();
        try {
            runnable.run();
        } finally {
            TenantAspect.enableCompanyCheck();
        }
    }
}