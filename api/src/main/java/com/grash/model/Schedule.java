package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.http.HttpStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Schedule entity defining recurrence patterns for preventive maintenance")
public class Schedule extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Whether the schedule is disabled")
    private boolean disabled;

    @NotNull
    @Schema(description = "Start date of the schedule", requiredMode = Schema.RequiredMode.REQUIRED)
    private Date startsOn = new Date();

    @NotNull
    @Schema(description = "Frequency value (e.g., every X days/weeks)", requiredMode = Schema.RequiredMode.REQUIRED)
    private int frequency = 1; //day

    @Schema(description = "End date of the schedule")
    private Date endsOn;

    @Schema(description = "Delay for due date in days")
    private Integer dueDateDelay;

    @Schema(description = "Indicates whether this is a demo schedule")
    private boolean isDemo;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Schema(description = "Type of recurrence", requiredMode = Schema.RequiredMode.REQUIRED)
    private RecurrenceType recurrenceType = RecurrenceType.DAILY;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Schema(description = "What the recurrence is based on", requiredMode = Schema.RequiredMode.REQUIRED)
    private RecurrenceBasedOn recurrenceBasedOn = RecurrenceBasedOn.SCHEDULED_DATE;

    @ElementCollection
    @Schema(description = "Days of week for weekly recurrence (0 = Monday)")
    private List<Integer> daysOfWeek = new ArrayList<>();//0 monday

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull
    private PreventiveMaintenance preventiveMaintenance;

    public Schedule(PreventiveMaintenance preventiveMaintenance) {
        this.preventiveMaintenance = preventiveMaintenance;
    }

    public void setFrequency(int frequency) {
        if (frequency < 1) throw new CustomException("Frequency should not be less than 1", HttpStatus.NOT_ACCEPTABLE);
        this.frequency = frequency;
    }

    public void setDueDateDelay(Integer dueDateDelay) {
        if (dueDateDelay != null && dueDateDelay < 1)
            throw new CustomException("Due date delay should not be less than 1", HttpStatus.NOT_ACCEPTABLE);
        this.dueDateDelay = dueDateDelay;
    }
}


