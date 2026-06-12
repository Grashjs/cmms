package com.grash.dto.shiftConfiguration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "User with their shift configuration")
public class UserShiftDTO {
    @Schema(description = "User ID")
    private Long userId;
    @Schema(description = "User full name")
    private String fullName;
    @Schema(description = "User's shift configuration")
    private ShiftConfigurationShowDTO shiftConfiguration;
}
