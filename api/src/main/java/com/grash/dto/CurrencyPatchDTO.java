package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing currency")
public class CurrencyPatchDTO {
    @Schema(description = "Currency name")
    private String name;
    
    @Schema(description = "Currency code")
    private String code;
}
