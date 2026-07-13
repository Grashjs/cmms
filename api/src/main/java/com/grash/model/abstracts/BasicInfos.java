package com.grash.model.abstracts;

import lombok.Data;

import javax.persistence.MappedSuperclass;
import javax.validation.constraints.NotNull;

@Data
@MappedSuperclass
public abstract class BasicInfos extends CompanyAudit {
    @NotNull
    private String name;
    private String address;
    private String phone;
    private String website;
    private String email;
}
