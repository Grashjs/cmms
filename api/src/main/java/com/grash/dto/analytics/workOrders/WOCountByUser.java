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
@Schema(description = "Work order count grouped by user")
public class WOCountByUser extends UserMiniDTO {
    @Schema(description = "Work order count")
    private int count;
}
