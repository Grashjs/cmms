package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.DateAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Schema(description = "Checklist entity containing a collection of tasks for work orders")
public class Checklist extends DateAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @OneToMany(orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<TaskBase> taskBases = new ArrayList<>();

    @Schema(description = "Name of the checklist", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = "Description of the checklist")
    private String description;

    @Schema(description = "Category of the checklist")
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private CompanySettings companySettings;

}


