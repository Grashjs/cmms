package com.grash.dto.shiftConfiguration;

import com.grash.model.ShiftDayConfiguration;
import com.grash.model.ShiftException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ShiftConfigurationPatchDTO {

    @Schema(description = "Default day configurations")
    private List<ShiftDayConfiguration> days;

    @Schema(description = "Date-specific exceptions")
    private List<ShiftException> exceptions;

    private boolean enabled;
}
