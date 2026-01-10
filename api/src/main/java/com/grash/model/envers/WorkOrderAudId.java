package com.grash.model.envers;

import lombok.Data;

import java.io.Serializable;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Embeddable
@Data
public class WorkOrderAudId implements Serializable {

    private static final long serialVersionUID = 1L;

    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "rev")
    private RevInfo rev;

}

