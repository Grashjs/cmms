package com.grash.dto;

import com.grash.model.Company;
import com.grash.dto.FileShowDTO;
import com.grash.model.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying full location details in API responses")
public class LocationShowDTO extends AuditShowDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Address")
    private String address;

    @Schema(description = "Longitude coordinate")
    private Double longitude;

    @Schema(description = "Latitude coordinate")
    private Double latitude;

    @Schema(description = "Indicates whether the location has child locations")
    private boolean hasChildren;

    @Schema(description = "List of teams assigned to the location")
    private List<TeamMiniDTO> teams = new ArrayList<>();

    @Schema(description = "Parent location")
    private Location parentLocation;

    @Schema(description = "List of vendors associated with the location")
    private List<VendorMiniDTO> vendors = new ArrayList<>();

    @Schema(description = "List of customers associated with the location")
    private List<CustomerMiniDTO> customers = new ArrayList<>();

    @Schema(description = "List of workers assigned to the location")
    private List<UserMiniDTO> workers = new ArrayList<>();

    @Schema(description = "Image file associated with the location")
    private FileShowDTO image;

    @Schema(description = "List of files attached to the location")
    private List<FileMiniDTO> files;

    @Schema(description = "Custom identifier")
    private String customId;

}
