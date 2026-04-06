package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Keygen user API response wrapper for list operations")
public class KeygenUserResponse {
    @Schema(description = "User data returned by the API")
    private KeygenUser data;
}
