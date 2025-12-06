package com.grash.dto;

import com.grash.dto.FileShowDTO;
import com.grash.model.PreventiveMaintenance;
import com.grash.model.enums.Status;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class WorkOrderShowDTO extends WorkOrderBaseShowDTO {

    private UserMiniDTO completedBy;

    private Date completedOn;

    private boolean archived;

    private RequestMiniDTO parentRequest;

    private PreventiveMaintenance parentPreventiveMaintenance;

    private String signature;

    private Status status;

    private String feedback;

    private FileShowDTO audioDescription;

    private String customId;
}
