package com.grash.dto.analytics.users;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User work order statistics")
public class UserWOStats {
    @Schema(description = "Number of work orders created")
    private int created;
    
    @Schema(description = "Number of work orders completed")
    private int completed;
}
