package com.grash.dto;

import com.grash.model.abstracts.WorkOrderBase;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Date;

@Data
@NoArgsConstructor
public class PreventiveMaintenancePostDTO extends WorkOrderBase {
    private Date startsOn;

    private String name;

    @NotNull
    private int frequency;

    private Integer dueDateDelay;

    private Date endsOn;
}
