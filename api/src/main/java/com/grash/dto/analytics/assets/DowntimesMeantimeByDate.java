package com.grash.dto.analytics.assets;

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
@Schema(description = "Downtime meantime metrics grouped by date")
public class DowntimesMeantimeByDate {
    @Schema(description = "Mean time in hours")
    private long meantime;
    
    @Schema(description = "Date")
    private Date date;
}
