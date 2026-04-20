package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring tax withholding information")
public class Withholdings {
    @Schema(description = "Whether tax withholdings are applied")
    public boolean taxWithholdings;
}
