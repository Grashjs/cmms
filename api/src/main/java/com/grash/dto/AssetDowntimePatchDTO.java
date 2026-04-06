package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching asset downtime information")
public class AssetDowntimePatchDTO {
    @Schema(description = "Duration of the asset downtime")
    private long duration;

    @Schema(description = "Start date of the downtime")
    private Date startsOn;
}
