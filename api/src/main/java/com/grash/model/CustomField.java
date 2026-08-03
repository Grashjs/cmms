package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.CustomFieldEntityType;
import com.grash.model.enums.CustomFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
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

    @Schema(description = "Unit of measure for numeric fields, e.g. m³/h or kW")
    @Column(length = 32)
    private String unit;

    /**
     * Asset categories this field belongs to. Empty means the field applies to every
     * asset, which is what all fields did before categories existed — so existing
     * configurations keep working untouched. Only meaningful for entityType ASSET.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "custom_field_asset_categories",
            joinColumns = @JoinColumn(name = "custom_field_id"),
            inverseJoinColumns = @JoinColumn(name = "asset_category_id"))
    @ToString.Exclude
    @Schema(description = "Asset categories this field applies to; empty means all assets")
    private List<AssetCategory> assetCategories = new ArrayList<>();

    @Schema(description = "Whether this field is required")
    private boolean required = false;

    @Schema(description = "Whether to copy this field value when repeating work orders")
    private boolean copyOnRepeat = false;

    @NotNull
    @Column(name = "field_order")
    private int order;

    @ElementCollection
    @CollectionTable(name = "company_custom_field_options", joinColumns = @JoinColumn(name = "field_id"))
    @Column(name = "option_value")
    @Schema(description = "Options for single choice fields")
    private List<String> options = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private CompanySettings companySettings;

}

