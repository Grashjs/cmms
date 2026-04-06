package com.grash.dto.analytics.requests;

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
@Schema(description = "Requests received and resolved grouped by date")
public class RequestsResolvedByDate {
    @Schema(description = "Number of requests received")
    private int received;
    
    @Schema(description = "Number of requests resolved")
    private int resolved;
    
    @Schema(description = "Date")
    private Date date;
}
