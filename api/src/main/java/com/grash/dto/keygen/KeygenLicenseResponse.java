package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeygenLicenseResponse {
    private KeygenLicenseResponseData data;
    private Object errors;
}
