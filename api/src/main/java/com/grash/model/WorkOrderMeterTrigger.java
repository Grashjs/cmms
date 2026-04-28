package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.WorkOrderBase;
import com.grash.model.enums.WorkOrderMeterTriggerCondition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.envers.NotAudited;
import org.springframework.http.HttpStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Work order meter trigger for generating work orders based on meter readings")
public class WorkOrderMeterTrigger extends WorkOrderBase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Whether the trigger is recurrent")
    private boolean recurrent;

    @NotNull
    @Schema(description = "Name of the meter trigger", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull
    @Schema(description = "Condition for triggering the work order", requiredMode = Schema.RequiredMode.REQUIRED)
    private WorkOrderMeterTriggerCondition triggerCondition;

    @NotNull
    @Schema(description = "Trigger value threshold", requiredMode = Schema.RequiredMode.REQUIRED)
    private int value;

    @NotNull
    @Schema(description = "Wait time before triggering", requiredMode = Schema.RequiredMode.REQUIRED)
    private int waitBefore;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Meter meter;

    @OneToMany(mappedBy = "workOrderMeterTrigger", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomFieldValue> customFieldValues = new ArrayList<>();

}



