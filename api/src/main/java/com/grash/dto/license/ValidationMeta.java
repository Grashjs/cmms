package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationMeta {
    private boolean valid;
    private String detail;
    private String code;
    private String ts;
    private ValidationScope scope;
}
