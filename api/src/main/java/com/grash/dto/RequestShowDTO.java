package com.grash.dto;

import com.grash.model.File;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestShowDTO extends WorkOrderBaseShowDTO {
    private boolean cancelled;

    private String cancellationReason;

    private WorkOrderMiniDTO workOrder;

    private FileMiniDTO audioDescription;

    private String customId;
}
