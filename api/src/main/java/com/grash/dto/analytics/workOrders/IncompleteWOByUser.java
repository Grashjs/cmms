package com.grash.dto.analytics.workOrders;

import com.grash.dto.UserMiniDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Incomplete work orders grouped by user")
public class IncompleteWOByUser extends UserMiniDTO {
    @Schema(description = "Count of incomplete work orders")
    private int count;
    
    @Schema(description = "Average age")
    private long averageAge;
}
