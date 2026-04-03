package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
public class ApiKey extends CompanyAudit {
    @NotNull
    private String label;
    @NotNull
    private String code;
    @ManyToOne
    @NotNull
    private OwnUser user;
    private Date lastUsed;
}