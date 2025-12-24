package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeygenLicenseData {
    private String type;
    private KeygenLicenseAttributes attributes;
    private KeygenLicenseRelationships relationships;
}
