package com.grash.dto.apiKey;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApiKeyPostDTO {
    @NotNull
    private String label;
}
