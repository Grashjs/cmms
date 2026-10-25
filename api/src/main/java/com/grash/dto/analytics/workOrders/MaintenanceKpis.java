package com.grash.dto.analytics.workOrders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Maintenance KPIs of a period")
public class MaintenanceKpis {

    @Schema(description = "Instant the backlog and the PM compliance are measured at: the end of the period, or now " +
            "if the period ends in the future")
    private Date asOf;

    @Schema(description = "Preventive maintenance work orders due in the period (up to asOf)")
    private int pmDue;

    @Schema(description = "Of pmDue, those completed on or before their due date")
    private int pmCompletedOnTime;

    @Schema(description = "pmCompletedOnTime / pmDue, in percent; null when nothing was due")
    private Double pmCompliance;

    @Schema(description = "Work orders created in the period by a preventive maintenance")
    private int planned;

    @Schema(description = "Other work orders created in the period (corrective, from requests)")
    private int reactive;

    @Schema(description = "planned / (planned + reactive), in percent; null when no work order was created")
    private Double plannedRatio;

    @Schema(description = "Work orders open at asOf (archived ones excluded)")
    private int backlog;

    @Schema(description = "Sum of the estimated duration of the backlog, in hours")
    private double backlogHours;

    @Schema(description = "Of the backlog, work orders past their due date at asOf")
    private int overdue;

    @Schema(description = "Response and resolution times of the reactive work orders created in the period, by " +
            "priority (highest first)")
    private List<PriorityResponseTimes> byPriority;
}
