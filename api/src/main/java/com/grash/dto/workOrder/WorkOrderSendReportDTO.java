package com.grash.dto.workOrder;

import com.grash.dto.ReportConfig;
import com.grash.model.Customer;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WorkOrderSendReportDTO {
    private ReportConfig config;
    private String message;
    @Size(min = 1)
    private List<Customer> customers;
}
