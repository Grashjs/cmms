package com.grash.dto.requestPortal;

import com.grash.dto.AuditShowDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Mini DTO for displaying basic request portal information")
public class RequestPortalMiniDTO extends AuditShowDTO {
    @Schema(description = "Title of the request portal")
    private String title;
    @Schema(description = "Unique identifier (UUID) of the request portal")
    private String uuid;
}