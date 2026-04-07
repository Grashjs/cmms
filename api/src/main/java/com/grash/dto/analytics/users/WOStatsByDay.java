package com.grash.dto.analytics.users;

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
@Schema(description = "Work order statistics grouped by day")
public class WOStatsByDay {
    @Schema(description = "Number of work orders created")
    private int created;
    
    @Schema(description = "Number of work orders completed")
    private int completed;
    
    @Schema(description = "Date")
    private Date date;
}
