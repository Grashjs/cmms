package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseValidationResponse {
    private LicenseData data;
    private ValidationMeta meta;
}
