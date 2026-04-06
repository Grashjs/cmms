package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching depreciation information")
public class DeprecationPatchDTO {

    @Schema(description = "Purchase price")
    private long purchasePrice;

    @Schema(description = "Purchase date")
    private Date purchaseDate;

    @Schema(description = "Residual value")
    private String residualValue;

    @Schema(description = "Useful life")
    private String usefulLIfe;

    @Schema(description = "Depreciation rate")
    private int rate;

    @Schema(description = "Current value")
    private long currentValue;
}
