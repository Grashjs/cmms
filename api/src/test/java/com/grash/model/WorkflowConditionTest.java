package com.grash.model;

import com.grash.model.enums.workflow.RequestCondition;
import com.grash.model.enums.workflow.WorkOrderCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers {@code TITLE_CONTAINS}, which the two switches in {@link WorkflowCondition} did not
 * handle although both enums carry it and the settings form offers it.
 *
 * <p>The failure mode is what makes this worth a test: an unhandled constant falls through to
 * {@code default: return false}, and because the engine requires <em>every</em> condition of a
 * rule to be met, one such condition silently disables the whole rule. No error, no log line,
 * nothing to notice. A test per handled constant is the only thing that catches the next one.
 */
class WorkflowConditionTest {

    private WorkflowCondition titleContains(String value) {
        WorkflowCondition condition = new WorkflowCondition();
        condition.setWorkOrderCondition(WorkOrderCondition.TITLE_CONTAINS);
        condition.setRequestCondition(RequestCondition.TITLE_CONTAINS);
        condition.setValue(value);
        return condition;
    }

    private WorkOrder workOrderTitled(String title) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setTitle(title);
        return workOrder;
    }

    private Request requestTitled(String title) {
        Request request = new Request();
        request.setTitle(title);
        return request;
    }

    @Nested
    @DisplayName("TITLE_CONTAINS on a work order")
    class WorkOrderTitle {

        @Test
        void matchesASubstring() {
            assertTrue(titleContains("Pumpe").isMetForWorkOrder(workOrderTitled("Störung Pumpe P-12")));
        }

        @Test
        void isCaseSensitive() {
            assertFalse(titleContains("pumpe").isMetForWorkOrder(workOrderTitled("Störung Pumpe P-12")));
        }

        @Test
        void doesNotMatchADifferentTitle() {
            assertFalse(titleContains("Pumpe").isMetForWorkOrder(workOrderTitled("Wartung Lüftung L-3")));
        }

        @Test
        @DisplayName("a rule saved without a value does not match instead of throwing")
        void toleratesAMissingValue() {
            assertFalse(titleContains(null).isMetForWorkOrder(workOrderTitled("Störung Pumpe P-12")));
        }

        @Test
        void toleratesAMissingTitle() {
            assertFalse(titleContains("Pumpe").isMetForWorkOrder(workOrderTitled(null)));
        }
    }

    @Nested
    @DisplayName("TITLE_CONTAINS on a request")
    class RequestTitle {

        @Test
        void matchesASubstring() {
            assertTrue(titleContains("Aufzug").isMetForRequest(requestTitled("Aufzug klemmt im 3. OG")));
        }

        @Test
        void doesNotMatchADifferentTitle() {
            assertFalse(titleContains("Aufzug").isMetForRequest(requestTitled("Heizung kalt")));
        }

        @Test
        void toleratesAMissingValue() {
            assertFalse(titleContains(null).isMetForRequest(requestTitled("Aufzug klemmt im 3. OG")));
        }

        @Test
        void toleratesAMissingTitle() {
            assertFalse(titleContains("Aufzug").isMetForRequest(requestTitled(null)));
        }
    }
}
