package com.grash.dto.apiKey;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ApiKeyPatchDTO {
    @NotNull
    private String label;
}
