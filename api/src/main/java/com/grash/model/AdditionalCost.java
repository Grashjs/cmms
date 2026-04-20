package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.Cost;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Additional cost entity representing extra expenses on a work order")
public class AdditionalCost extends Cost {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Description of the additional cost")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private User assignedTo;

    @Schema(description = "Whether to include this cost in the total")
    private boolean includeToTotalCost;

    @Schema(description = "Date of the additional cost")
    private Date date;

    @Schema(description = "Indicates whether this is a demo record")
    private boolean isDemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private WorkOrder workOrder;
}


