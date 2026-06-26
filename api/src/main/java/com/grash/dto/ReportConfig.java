package com.grash.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportConfig {
    private boolean cost = true;
    private boolean comments = true;
    private boolean tasks = true;
    private boolean workOrderHistory = true;
    private boolean estimatedTime = true;
    private boolean locationAddress = true;
    private boolean priority = true;
    private boolean workOrderInformation = true;
    private boolean relations = true;
    private boolean files = true;
    private boolean signature = true;
}
