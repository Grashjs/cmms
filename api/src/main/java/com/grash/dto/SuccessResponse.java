package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic success response")
public class SuccessResponse {
    @Schema(description = "Whether the operation was successful")
    private boolean success;
    
    @Schema(description = "Response message")
    private String message;
}
