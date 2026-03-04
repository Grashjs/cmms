package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class RequestPortal extends CompanyAudit {
    @NotBlank
    private String title;
    private String welcomeMessage;
    @NotNull
    private String uuid;

    @OneToMany(mappedBy = "requestPortal")
    private List<RequestPortalField> fields;
}