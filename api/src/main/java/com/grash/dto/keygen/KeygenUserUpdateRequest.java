package com.grash.dto.keygen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Keygen user update API request")
public class KeygenUserUpdateRequest {
    @Schema(description = "User update data")
    private KeygenUserUpdateData data;
}
