package com.grash.dto.requestPortal;

import com.grash.dto.AssetMiniDTO;
import com.grash.dto.LocationMiniDTO;
import com.grash.model.enums.PortalFieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for displaying a request portal field")
public class RequestPortalFieldShowDTO {
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Type of the portal field")
    private PortalFieldType type;

    @Schema(description = "Location associated with the field")
    private LocationMiniDTO location;

    @Schema(description = "Asset associated with the field")
    private AssetMiniDTO asset;

    @Schema(description = "Whether this field is required")
    private boolean required;
}
