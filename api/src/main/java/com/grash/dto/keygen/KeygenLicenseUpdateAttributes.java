package com.grash.dto.keygen;

import lombok.Data;

import java.util.Map;

@Data
public class KeygenLicenseUpdateAttributes {
    private String expiry;
    private Map<String, Object> metadata;
}