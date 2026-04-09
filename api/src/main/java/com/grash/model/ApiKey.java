package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "API key entity for authentication and authorization")
public class ApiKey extends CompanyAudit {
    @Schema(description = "Label of the API key", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String label;
    @Schema(description = "API key code", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String code;
    @ManyToOne
    @NotNull
    private User user;
    @Schema(description = "Last usage date", accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastUsed;
}