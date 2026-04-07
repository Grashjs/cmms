package com.grash.dto.paddle;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle checkout information")
public class Checkout {
    @Schema(description = "Checkout URL")
    private String url;
}
