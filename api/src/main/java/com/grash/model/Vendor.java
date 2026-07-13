package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.BasicInfos;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Vendor extends BasicInfos {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String vendorType;

    @NotNull
    private String companyName;

    private String description;

    private long rate;

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
}
