package com.grash.dto.paddle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle quantity constraints for a price")
public class Quantity {
    @Schema(description = "Minimum allowed quantity")
    private Integer minimum;

    @Schema(description = "Maximum allowed quantity")
    private Integer maximum;
}
