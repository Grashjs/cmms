package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen license response attributes with full license details")
public class KeygenLicenseResponseAttributes {
    @Schema(description = "License name")
    private String name;
    @Schema(description = "License key string")
    private String key;
    @Schema(description = "License expiry date and time")
    private OffsetDateTime expiry;
    @Schema(description = "License status (e.g., active, expired, suspended)")
    private String status;
    @Schema(description = "Number of times the license has been used")
    private Integer uses;
    @Schema(description = "Whether the license is currently suspended")
    private Boolean suspended;
    @Schema(description = "Whether the license key is encrypted")
    private Boolean encrypted;
    @Schema(description = "Whether strict enforcement is enabled")
    private Boolean strict;
    @Schema(description = "Whether this is a floating license")
    private Boolean floating;
    @Schema(description = "Whether the license is protected")
    private Boolean protectedLicense;
    @Schema(description = "Maximum number of machines allowed")
    private Integer maxMachines;
    @Schema(description = "Maximum number of processes allowed")
    private Integer maxProcesses;
    @Schema(description = "Maximum number of users allowed")
    private Integer maxUsers;
    @Schema(description = "Maximum number of CPU cores allowed")
    private Integer maxCores;
    @Schema(description = "Maximum memory allowed (in bytes)")
    private Integer maxMemory;
    @Schema(description = "Maximum disk space allowed (in bytes)")
    private Integer maxDisk;
    @Schema(description = "Maximum number of uses allowed")
    private Integer maxUses;
    @Schema(description = "Whether heartbeat validation is required")
    private Boolean requireHeartbeat;
    @Schema(description = "Whether check-in validation is required")
    private Boolean requireCheckIn;
    @Schema(description = "Date and time of the last validation")
    private OffsetDateTime lastValidated;
    @Schema(description = "Date and time of the last check-in")
    private OffsetDateTime lastCheckIn;
    @Schema(description = "Date and time of the next required check-in")
    private OffsetDateTime nextCheckIn;
    @Schema(description = "Date and time of the last check-out")
    private OffsetDateTime lastCheckOut;
    @Schema(description = "List of permissions granted by this license")
    private List<String> permissions;
    @Schema(description = "Custom metadata key-value pairs")
    private Map<String, Object> metadata;
    @Schema(description = "Date and time the license was created")
    private OffsetDateTime created;
    @Schema(description = "Date and time the license was last updated")
    private OffsetDateTime updated;
}
