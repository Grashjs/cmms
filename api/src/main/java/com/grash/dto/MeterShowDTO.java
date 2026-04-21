package com.grash.dto;

import com.grash.dto.FileShowDTO;
import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import com.grash.model.MeterCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying full meter details in API responses")
public class MeterShowDTO extends AuditShowDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Unit of measurement")
    private String unit;

    @Schema(description = "Update frequency")
    private int updateFrequency;

    @Schema(description = "Meter category")
    private CategoryMiniDTO meterCategory;

    @Schema(description = "Image file associated with the meter")
    private FileShowDTO image;

    @Schema(description = "List of users who have access to the meter")
    private List<UserMiniDTO> users = new ArrayList<>();

    @Schema(description = "Location where the meter is installed")
    private LocationMiniDTO location;

    @Schema(description = "Asset associated with the meter")
    private AssetMiniDTO asset;

    @Schema(description = "Last reading date")
    private Date lastReading;

    @Schema(description = "Next reading date")
    private Date nextReading;

    @Schema(description = "Custom field values")
    private List<CustomFieldValueShowDTO> customFieldValues = new ArrayList<>();
}
