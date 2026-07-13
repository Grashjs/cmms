package com.grash.dto;

import com.grash.model.*;
import com.grash.model.enums.AssetStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Date;

@Data
@NoArgsConstructor
public class AssetPatchDTO {
    private boolean archived;

    private File image;

    private Location location;

    private Asset parentAsset;

    private String area;

    private String barCode;

    private String nfcId;

    private AssetCategory category;

    private String name;

    private OwnUser primaryUser;

    private Deprecation deprecation;

    private Date warrantyExpirationDate;

    private String additionalInfos;

    private String serialNumber;

    private Collection<OwnUser> assignedTo;

    private Collection<Customer> customers;

    private Collection<Vendor> vendors;

    private Collection<Team> teams;

    private Collection<File> files;

    private Collection<Part> parts;

    private AssetStatus status;

    private Double acquisitionCost;

    private String power;

    private String manufacturer;

    private String model;

    private String description;

    private Date inServiceDate;
}
