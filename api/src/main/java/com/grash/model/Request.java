package com.grash.model;

import com.grash.model.abstracts.WorkOrderBase;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
public class Request extends WorkOrderBase {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String customId;

    private boolean cancelled;

    private String cancellationReason;

    private boolean isDemo;

    @OneToOne(fetch = FetchType.LAZY)
    private File audioDescription;

    @OneToOne(fetch = FetchType.LAZY)
    private WorkOrder workOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    private RequestPortal requestPortal;

    @PreRemove
    private void preRemove() {
        if (workOrder != null)
            workOrder.setParentRequest(null);
    }

}

