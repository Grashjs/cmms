package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicenseValidationScope {
    private String fingerprint;
}
