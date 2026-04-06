package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Keygen user creation API request")
public class KeygenUserCreateRequest {
    @Schema(description = "User creation data")
    private KeygenUserCreateData data;
}
