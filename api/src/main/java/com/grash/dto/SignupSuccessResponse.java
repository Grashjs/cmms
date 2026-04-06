package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Response returned after a successful user signup")
public class SignupSuccessResponse<T> {
    @Schema(description = "Whether the signup was successful")
    private boolean success;
    
    @Schema(description = "Response message")
    private String message;
    
    @Schema(description = "User data")
    private T user;
}
