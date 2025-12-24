package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeygenLicenseResponseAttributes {
    private String name;
    private String key;
    private OffsetDateTime expiry;
    private String status;

    private Integer uses;
    private Boolean suspended;
    private Boolean encrypted;
    private Boolean strict;
    private Boolean floating;
    private Boolean protectedLicense;

    private Integer maxMachines;
    private Integer maxProcesses;
    private Integer maxUsers;
    private Integer maxCores;
    private Integer maxMemory;
    private Integer maxDisk;
    private Integer maxUses;

    private Boolean requireHeartbeat;
    private Boolean requireCheckIn;

    private OffsetDateTime lastValidated;
    private OffsetDateTime lastCheckIn;
    private OffsetDateTime nextCheckIn;
    private OffsetDateTime lastCheckOut;

    private List<String> permissions;
    private Map<String, Object> metadata;

    private OffsetDateTime created;
    private OffsetDateTime updated;
}
