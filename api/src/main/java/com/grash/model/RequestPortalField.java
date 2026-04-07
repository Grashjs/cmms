package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.PortalFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Request portal field for customizing portal forms")
public class RequestPortalField extends CompanyAudit {
    @NotNull
    @Enumerated(EnumType.STRING)
    @Schema(description = "Type of the portal field", requiredMode = Schema.RequiredMode.REQUIRED)
    private PortalFieldType type;

    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    private Asset asset;

    @Schema(description = "Whether this field is required")
    private boolean required;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JsonIgnore
    private RequestPortal requestPortal;
}