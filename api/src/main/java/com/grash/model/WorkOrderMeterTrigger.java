package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.http.HttpStatus;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class WorkOrderMeterTrigger extends WorkOrderBase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private boolean recurrent;

    @NotNull
    private String name;

    @NotNull
    private WorkOrderMeterTriggerCondition triggerCondition;

    @NotNull
    private int value;

    @NotNull
    private int waitBefore;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Meter meter;
}

