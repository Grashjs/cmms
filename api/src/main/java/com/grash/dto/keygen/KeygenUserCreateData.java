package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeygenUserCreateData {
    private String type;
    private KeygenUserCreateAttributes attributes;
}
