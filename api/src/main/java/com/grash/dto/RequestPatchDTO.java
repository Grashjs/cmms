package com.grash.dto;

import com.grash.model.File;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestPatchDTO extends WorkOrderBasePatchDTO {
    private boolean cancelled;

    private File audioDescription;
}
