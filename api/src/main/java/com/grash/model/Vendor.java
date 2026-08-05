package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.BasicInfos;
import com.grash.model.enums.PermissionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Vendor entity for managing supplier information")
public class Vendor extends BasicInfos {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Type of vendor")
    private String vendorType;

    @NotNull
    @Schema(description = "Company name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String companyName;

    @Schema(description = "Vendor description")
    private String description;

    @Schema(description = "Vendor rate")
    private long rate;

    @Schema(description = "Indicates whether this is a demo vendor")
    private boolean isDemo;

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Asset_Vendor_Associations",
            joinColumns = @JoinColumn(name = "id_vendor"),
            inverseJoinColumns = @JoinColumn(name = "id_asset"),
            indexes = {
                    @Index(name = "idx_vendor_asset_vendor_id", columnList = "id_vendor"),
                    @Index(name = "idx_vendor_asset_asset_id", columnList = "id_asset")
            })
    private List<Asset> assets = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Location_Vendor_Associations",
            joinColumns = @JoinColumn(name = "id_vendor"),
            inverseJoinColumns = @JoinColumn(name = "id_location"),
            indexes = {
                    @Index(name = "idx_vendor_location_vendor_id", columnList = "id_vendor"),
                    @Index(name = "idx_vendor_location_location_id", columnList = "id_location")
            })
    private List<Location> locations = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Part_vendor_Associations",
            joinColumns = @JoinColumn(name = "id_vendor"),
            inverseJoinColumns = @JoinColumn(name = "id_part"),
            indexes = {
                    @Index(name = "idx_vendor_part_vendor_id", columnList = "id_vendor"),
                    @Index(name = "idx_vendor_part_part_id", columnList = "id_part")
            })
    private List<Part> parts = new ArrayList<>();

    @OneToMany(mappedBy = "vendor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CustomFieldValue> customFieldValues = new ArrayList<>();

    public boolean canBeEditedBy(User user) {
        return user.getRole().getEditOtherPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId()));
    }

    public boolean canBeDeletedBy(User user) {
        return user.getRole().getDeleteOtherPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS)
                || (this.getCreatedBy() != null && this.getCreatedBy().equals(user.getId()));
    }

    public boolean canBeViewedBy(User user) {
        return (user.getRole().getViewPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS) &&
                (user.getRole().getViewOtherPermissions().contains(PermissionEntity.VENDORS_AND_CUSTOMERS) || (getCreatedBy() != null && getCreatedBy().equals(user.getId()))));
    }
}


