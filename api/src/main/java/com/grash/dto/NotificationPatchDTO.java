package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching a notification")
public class NotificationPatchDTO {
    @Schema(description = "Whether the notification has been seen")
    private boolean seen;
}
