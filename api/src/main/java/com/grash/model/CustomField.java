package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Custom field configuration for company settings")
public class CustomField extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Field label", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;

    @NotNull
    @Schema(description = "Field type", requiredMode = Schema.RequiredMode.REQUIRED)
    @Enumerated(EnumType.STRING)
    private CustomFieldType fieldType;

    @Schema(description = "Entity type this field applies to")
    @Enumerated(EnumType.STRING)
    @NotNull
    private CustomFieldEntityType entityType;

    @Schema(description = "Whether this field is required")
    private boolean required = false;

    @Schema(description = "Whether to copy this field value when repeating work orders")
    private boolean copyOnRepeat = false;

    @ElementCollection
    @CollectionTable(name = "company_custom_field_options", joinColumns = @JoinColumn(name = "field_id"))
    @Column(name = "option_value")
    @Schema(description = "Options for single choice fields")
    private List<String> options = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private CompanySettings companySettings;

}

