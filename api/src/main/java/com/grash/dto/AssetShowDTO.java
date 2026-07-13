package com.grash.dto;

import com.grash.model.AssetCategory;
import com.grash.model.Deprecation;
import com.grash.dto.FileShowDTO;
import com.grash.model.enums.AssetStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class AssetShowDTO extends AuditShowDTO {

    private boolean archived;

    private boolean hasChildren;

    private String description;

    private FileShowDTO image;

    private LocationMiniDTO location;

    private AssetMiniDTO parentAsset;

    private String area;

    private String barCode;

    private AssetCategory category;

    private String name;

    private UserMiniDTO primaryUser;

    private List<UserMiniDTO> assignedTo = new ArrayList<>();

    private List<TeamMiniDTO> teams = new ArrayList<>();

    private List<VendorMiniDTO> vendors = new ArrayList<>();

    private List<CustomerMiniDTO> customers = new ArrayList<>();

    private Deprecation deprecation;

    private Date warrantyExpirationDate;

    private Date inServiceDate;

    private String additionalInfos;

    private String serialNumber;

    private String model;

    private AssetStatus status = AssetStatus.OPERATIONAL;

    private Double acquisitionCost;

    private List<FileMiniDTO> files = new ArrayList<>();

    private List<PartMiniDTO> parts = new ArrayList<>();

    private String power;

    private String manufacturer;

    private String customId;
}
