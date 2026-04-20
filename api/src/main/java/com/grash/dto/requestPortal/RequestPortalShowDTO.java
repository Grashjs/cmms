package com.grash.dto.requestPortal;

import com.grash.model.RequestPortalField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.grash.dto.AuditShowDTO;

import java.util.List;

@Data
@Schema(description = "DTO for displaying request portal details in API responses")
public class RequestPortalShowDTO extends AuditShowDTO {
    @Schema(description = "Title of the request portal")
    private String title;
    
    @Schema(description = "Welcome message displayed on the portal")
    private String welcomeMessage;
    
    @Schema(description = "Unique identifier (UUID) of the portal")
    private String uuid;
    
    @Schema(description = "List of custom fields in the portal")
    private List<RequestPortalField> fields;
}
