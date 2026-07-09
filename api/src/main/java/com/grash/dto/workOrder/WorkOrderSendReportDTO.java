package com.grash.dto.workOrder;

import com.grash.dto.ReportConfig;
import lombok.Data;

@Data
public class WorkOrderSendReportDTO {
    private ReportConfig config;
    private String message;
}
