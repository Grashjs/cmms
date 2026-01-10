package com.grash.dto;

import com.grash.model.File;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestPatchDTO extends WorkOrderBasePatchDTO {
    private boolean cancelled;

    @Schema(implementation = IdDTO.class)
    private File audioDescription;
}
