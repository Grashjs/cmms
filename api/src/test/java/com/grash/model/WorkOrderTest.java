package com.grash.model;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkOrderTest {

    private static final long HOUR = TimeUnit.HOURS.toMillis(1);

    @Test
    void getAverageAge_keepsFractionsOfADay() {
        WorkOrder completedIn20Hours = completed(0, 20 * HOUR);

        assertEquals(20.0 / 24, WorkOrder.getAverageAge(List.of(completedIn20Hours)), 1e-9);
    }

    @Test
    void getAverageAge_averagesWithoutIntegerDivision() {
        WorkOrder completedIn12Hours = completed(0, 12 * HOUR);
        WorkOrder completedIn36Hours = completed(0, 36 * HOUR);
        WorkOrder notCompleted = completed(0, 0);
        notCompleted.setCompletedOn(null);

        assertEquals(1.0, WorkOrder.getAverageAge(List.of(completedIn12Hours, completedIn36Hours, notCompleted)), 1e-9);
    }

    @Test
    void getAverageAge_startsFromTheParentRequest() {
        WorkOrder workOrder = completed(24 * HOUR, 48 * HOUR);
        Request request = new Request();
        request.setCreatedAt(new Date(0));
        workOrder.setParentRequest(request);

        assertEquals(2.0, WorkOrder.getAverageAge(List.of(workOrder)), 1e-9);
    }

    @Test
    void getAverageAge_empty_returnsZero() {
        assertEquals(0.0, WorkOrder.getAverageAge(List.of()));
    }

    private static WorkOrder completed(long createdAtMillis, long completedOnMillis) {
        WorkOrder workOrder = new WorkOrder();
        workOrder.setCreatedAt(new Date(createdAtMillis));
        workOrder.setCompletedOn(new Date(completedOnMillis));
        return workOrder;
    }
}
