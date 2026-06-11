package com.grash.dto.shiftConfiguration;

import com.grash.dto.AuditShowDTO;
import com.grash.model.ShiftDayConfiguration;
import com.grash.model.ShiftException;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class ShiftConfigurationShowDTO extends AuditShowDTO {

    @Schema(description = "Default day configurations")
    private List<ShiftDayConfiguration> days = new ArrayList<>();

    @Schema(description = "Date-specific exceptions")
    private List<ShiftException> exceptions = new ArrayList<>();

    private boolean enabled;
}
