package com.grash.dto.workload;

import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@Schema(description = "Unscheduled work orders grouped by status")
public class UnscheduledWorkOrdersDTO {
    @Schema(description = "Count of unscheduled work orders by status")
    private Map<Status, Integer> statusCounts = new HashMap<>();
    @Schema(description = "Count of overdue work orders")
    private int overdueCount;
    @Schema(description = "Count of work orders due soon (within 48 hours)")
    private int dueSoonCount;
    @Schema(description = "List of unscheduled work orders")
    private List<WorkloadWorkOrderDTO> workOrders = new ArrayList<>();
}
