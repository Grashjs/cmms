package com.grash.dto.requestPortal;

import com.grash.model.RequestPortalField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing request portal")
public class RequestPortalPatchDTO {
    @Schema(description = "Title of the request portal")
    @NotBlank
    private String title;
    
    @Schema(description = "Welcome message displayed on the portal")
    private String welcomeMessage;
    
    @Schema(description = "List of custom fields in the portal")
    private List<RequestPortalField> fields;
}
