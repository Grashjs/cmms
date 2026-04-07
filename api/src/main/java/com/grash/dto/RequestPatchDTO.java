package com.grash.dto;

import com.grash.model.File;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing maintenance request")
public class RequestPatchDTO extends WorkOrderBasePatchDTO {
    @Schema(description = "Whether the request has been cancelled")
    private boolean cancelled;

    @Schema(description = "Audio description file attached to the request", implementation = IdDTO.class)
    private File audioDescription;
}
