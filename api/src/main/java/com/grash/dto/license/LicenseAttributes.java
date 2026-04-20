package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Core license attributes including restrictions, permissions, and metadata")
public class LicenseAttributes {
    @Schema(description = "Display name of the license")
    private String name;
    @Schema(description = "Unique license key")
    private String key;
    @Schema(description = "Expiration date of the license")
    private String expiry;
    @Schema(description = "Current status of the license (e.g., ACTIVE, EXPIRED)")
    private String status;
    @Schema(description = "Whether the license is suspended")
    private Boolean suspended;
    @Schema(description = "Version of the license")
    private String version;
    @Schema(description = "Whether the license supports floating usage")
    private Boolean floating;
    @Schema(description = "Maximum number of machines allowed")
    private Integer maxMachines;
    @Schema(description = "Maximum number of processes allowed")
    private Integer maxProcesses;
    @Schema(description = "Maximum number of users allowed")
    private Integer maxUsers;
    @Schema(description = "Maximum number of CPU cores allowed")
    private Integer maxCores;
    @Schema(description = "Timestamp of last validation")
    private String lastValidated;
    @Schema(description = "List of permissions granted by the license")
    private List<String> permissions;
    @Schema(description = "Additional metadata associated with the license")
    private Map<String, Object> metadata;
    @Schema(description = "Timestamp when the license was created")
    private String created;
    @Schema(description = "Timestamp when the license was last updated")
    private String updated;
}
