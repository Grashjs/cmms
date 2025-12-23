package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseData {
    private String id;
    private String type;
    private LicenseAttributes attributes;
    private LicenseRelationships relationships;
    private Map<String, String> links;
}
