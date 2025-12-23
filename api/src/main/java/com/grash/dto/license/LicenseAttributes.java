package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseAttributes {
    private String name;
    private String key;
    private String expiry;
    private String status;
    private Boolean suspended;
    private String version;
    private Boolean floating;
    private Integer maxMachines;
    private Integer maxProcesses;
    private Integer maxUsers;
    private Integer maxCores;
    private String lastValidated;
    private List<String> permissions;
    private Map<String, Object> metadata;
    private String created;
    private String updated;
}
