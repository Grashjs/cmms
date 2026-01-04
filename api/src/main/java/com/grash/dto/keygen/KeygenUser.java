package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeygenUser {
    private String id;
    private String type;
    private KeygenUserAttributes attributes;
}
