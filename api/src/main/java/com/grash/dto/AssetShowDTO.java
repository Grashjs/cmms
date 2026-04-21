package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import com.grash.model.AssetCategory;
import com.grash.model.Deprecation;
import com.grash.dto.FileShowDTO;
import com.grash.model.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying full asset details in API responses")
public class AssetShowDTO extends AuditShowDTO {

    @Schema(description = "Indicates whether the asset is archived")
    private boolean archived;

    @Schema(description = "Indicates whether the asset has child assets")
    private boolean hasChildren;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Image file associated with the asset")
    private FileShowDTO image;

    @Schema(description = "Location information")
    private LocationMiniDTO location;

    @Schema(description = "Parent asset information")
    private AssetMiniDTO parentAsset;

    @Schema(description = "Area or zone where the asset is located")
    private String area;

    @Schema(description = "Barcode identifier for the asset")
    private String barCode;

    @Schema(description = "NFC identifier for the asset")
    private String nfcId;

    @Schema(description = "Category of the asset")
    private CategoryMiniDTO category;

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Primary user responsible for the asset")
    private UserMiniDTO primaryUser;

    @Schema(description = "List of users assigned to the asset")
    private List<UserMiniDTO> assignedTo = new ArrayList<>();

    @Schema(description = "List of teams assigned to the asset")
    private List<TeamMiniDTO> teams = new ArrayList<>();

    @Schema(description = "List of vendors associated with the asset")
    private List<VendorMiniDTO> vendors = new ArrayList<>();

    @Schema(description = "List of customers associated with the asset")
    private List<CustomerMiniDTO> customers = new ArrayList<>();

    @Schema(description = "Depreciation configuration for the asset")
    private Deprecation deprecation;

    @Schema(description = "Warranty expiration date")
    private Date warrantyExpirationDate;

    @Schema(description = "Date when the asset was placed into service")
    private Date inServiceDate;

    @Schema(description = "Additional information about the asset")
    private String additionalInfos;

    @Schema(description = "Serial number of the asset")
    private String serialNumber;

    @Schema(description = "Model of the asset")
    private String model;

    @Schema(description = "Current status of the asset")
    private AssetStatus status = AssetStatus.OPERATIONAL;

    @Schema(description = "Acquisition cost of the asset")
    private Double acquisitionCost;

    @Schema(description = "List of files attached to the asset")
    private List<FileMiniDTO> files = new ArrayList<>();

    @Schema(description = "List of parts associated with the asset")
    private List<PartMiniDTO> parts = new ArrayList<>();

    @Schema(description = "Power rating or specification of the asset")
    private String power;

    @Schema(description = "Manufacturer of the asset")
    private String manufacturer;

    @Schema(description = "Custom identifier")
    private String customId;

    @Schema(description = "Custom field values")
    private List<CustomFieldValueShowDTO> customFieldValues = new ArrayList<>();
}
