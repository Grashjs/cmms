package com.grash.model;

import com.grash.model.abstracts.CategoryAbstract;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Schema(description = "Subscription change request for plan modifications")
public class SubscriptionChangeRequest extends CompanyAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Subscription plan code", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @NotNull
    @Schema(description = "Whether monthly billing is requested", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean monthly;

    @NotNull
    @Schema(description = "Requested number of users", requiredMode = Schema.RequiredMode.REQUIRED)
    private int usersCount;
}


