package com.grash.dto.analytics.workOrders;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Maintenance KPIs of one segment of a period")
public class MaintenanceKpisByDate {

    @Schema(description = "End of the segment; the backlog is measured at this instant")
    private Date date;

    @Schema(description = "Work orders open at date (archived ones excluded)")
    private int backlog;

    @Schema(description = "Sum of the estimated duration of the backlog, in hours")
    private double backlogHours;

    @Schema(description = "Of the backlog, work orders past their due date")
    private int overdue;

    @Schema(description = "Work orders created in the segment by a preventive maintenance")
    private int planned;

    @Schema(description = "Other work orders created in the segment")
    private int reactive;
}
