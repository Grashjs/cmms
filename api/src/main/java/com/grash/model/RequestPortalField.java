package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.PortalFieldType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
public class RequestPortalField extends CompanyAudit {
    @NotNull
    @Enumerated(EnumType.STRING)
    private PortalFieldType type;

    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    private Asset asset;

    private boolean required;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private RequestPortal requestPortal;
}