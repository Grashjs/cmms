package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.Cost;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@NoArgsConstructor
public class AdditionalCost extends Cost {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    private OwnUser assignedTo;

    private boolean includeToTotalCost;

    private Date date;

    private boolean isDemo;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private WorkOrder workOrder;
}
