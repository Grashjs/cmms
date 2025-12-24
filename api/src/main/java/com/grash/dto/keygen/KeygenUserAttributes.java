package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeygenUserAttributes {
    private String email;
    private Map<String, Object> metadata;
}
