package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Keygen user API response wrapper for single user operations")
public class KeygenUserSingleResponse {
    @Schema(description = "Single user data returned by the API")
    private KeygenUser data;
}
