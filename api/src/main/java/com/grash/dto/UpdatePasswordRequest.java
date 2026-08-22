package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.grash.validation.ValidPassword;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Request for updating user password")
public class UpdatePasswordRequest {
    @Schema(description = "Current password")
    @NotNull
    @Size(max = 128)
    private String oldPassword;

    @Schema(description = "New password")
    @NotNull
    @ValidPassword
    private String newPassword;
}

