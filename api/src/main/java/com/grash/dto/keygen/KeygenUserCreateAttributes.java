// KeygenUserCreateAttributes.java
package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KeygenUserCreateAttributes {
    private String email;
    private String firstName;
    private String lastName;
    private Map<String, String> metadata;
}
