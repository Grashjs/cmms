package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeygenLicenseResponseData {
    private String id;
    private String type;
    private KeygenLicenseResponseAttributes attributes;
    private KeygenLicenseRelationships relationships;
}
