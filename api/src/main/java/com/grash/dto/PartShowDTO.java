package com.grash.dto;

import com.grash.dto.FileShowDTO;
import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import com.grash.model.PartCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying full part details in API responses")
public class PartShowDTO extends AuditShowDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Cost")
    private double cost;

    @Schema(description = "Category")
    private CategoryMiniDTO category;

    @Schema(description = "Indicates whether this is a non-stock part")
    private boolean nonStock;

    @Schema(description = "Barcode identifier")
    private String barcode;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Current quantity in stock")
    private double quantity;

    @Schema(description = "Additional information")
    private String additionalInfos;

    @Schema(description = "Area or storage location")
    private String area;

    @Schema(description = "Minimum quantity threshold")
    private double minQuantity;

    @Schema(description = "Location where the part is stored")
    private LocationMiniDTO location;

    @Schema(description = "Image file")
    private FileShowDTO image;

    @Schema(description = "List of users assigned to the part")
    private Collection<UserMiniDTO> assignedTo;

    @Schema(description = "List of files attached to the part")
    private Collection<FileShowDTO> files;

    @Schema(description = "List of customers associated with the part")
    private Collection<CustomerMiniDTO> customers;

    @Schema(description = "List of vendors associated with the part")
    private Collection<VendorMiniDTO> vendors;

    @Schema(description = "List of teams assigned to the part")
    private Collection<TeamMiniDTO> teams;

    @Schema(description = "Unit of measurement")
    private String unit;

    @Schema(description = "Custom field values")
    private List<CustomFieldValueShowDTO> customFieldValues;
}
