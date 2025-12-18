package com.grash.dto;

import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.ElementCollection;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class PreventiveMaintenancePostDTO extends WorkOrderBase {
    private Date startsOn;

    @NotNull
    private String name;

    @NotNull
    private int frequency;

    private Integer dueDateDelay;

    private Date endsOn;

    @NotNull
    private RecurrenceType recurrenceType;

    @NotNull
    private RecurrenceBasedOn recurrenceBasedOn;

    private List<Integer> daysOfWeek = new ArrayList<>();
}
