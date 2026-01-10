package com.grash.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdatePasswordRequest {
    @NotNull
    @Size(min = 6, max = 50)
    private String oldPassword;
    @NotNull
    @Size(min = 6, max = 50)
    private String newPassword;
}

