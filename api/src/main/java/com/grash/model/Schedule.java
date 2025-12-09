package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.RecurrenceBasedOn;
import com.grash.model.enums.RecurrenceType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.http.HttpStatus;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Schedule extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private boolean disabled;

    @NotNull
    private Date startsOn = new Date();

    @NotNull
    private int frequency = 1; //day

    private Date endsOn;

    private Integer dueDateDelay;

    private boolean isDemo;

    @Enumerated(EnumType.STRING)
    @NotNull
    private RecurrenceType recurrenceType = RecurrenceType.DAILY;

    @Enumerated(EnumType.STRING)
    @NotNull
    private RecurrenceBasedOn recurrenceBasedOn = RecurrenceBasedOn.SCHEDULED_DATE;

    @ElementCollection
    private List<Integer> daysOfWeek = new ArrayList<>();//0 monday

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
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
