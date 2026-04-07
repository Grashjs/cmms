package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request for updating user password")
public class UpdatePasswordRequest {
    @Schema(description = "Current password")
    @NotNull
    @Size(min = 6, max = 50)
    private String oldPassword;
    
    @Schema(description = "New password")
    @NotNull
    @Size(min = 6, max = 50)
    private String newPassword;
}

