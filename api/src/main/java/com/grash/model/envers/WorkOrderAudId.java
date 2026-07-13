package com.grash.model.envers;

import lombok.Data;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
