package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntitlementData {
    private String id;
    private String type;
    private EntitlementAttributes attributes;
}
