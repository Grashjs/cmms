// KeygenUserCreateAttributes.java
package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Keygen user creation attributes")
public class KeygenUserCreateAttributes {
    @Schema(description = "User email address")
    private String email;
    @Schema(description = "User first name")
    private String firstName;
    @Schema(description = "User last name")
    private String lastName;
    @Schema(description = "Custom metadata key-value pairs")
    private Map<String, String> metadata;
}
