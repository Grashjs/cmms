package com.grash.model;

import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.CascadeType;
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
@Schema(description = "Request portal entity for external user submissions")
public class RequestPortal extends CompanyAudit {
    @NotBlank
    @Schema(description = "Portal title", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;
    @Schema(description = "Welcome message displayed on the portal")
    private String welcomeMessage;
    @NotNull
    @Schema(description = "Unique UUID for the portal", requiredMode = Schema.RequiredMode.REQUIRED)
    private String uuid;

    @OneToMany(mappedBy = "requestPortal", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RequestPortalField> fields;
}