package com.grash.dto.requestPortal;

import com.grash.dto.AuditShowDTO;
import com.grash.model.RequestPortalField;
import com.grash.model.enums.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Public DTO for displaying request portal information (without authentication)")
public class RequestPortalPublicDTO extends AuditShowDTO {
    @Schema(description = "Title of the request portal")
    private String title;
    
    @Schema(description = "Welcome message displayed on the portal")
    private String welcomeMessage;
    
    @Schema(description = "List of custom fields in the portal")
    private List<RequestPortalField> fields;
    
    @Schema(description = "Unique identifier (UUID) of the portal")
    private String uuid;
    
    @Schema(description = "Company ID")
    private Long companyId;
    
    @Schema(description = "Company name")
    private String companyName;
    
    @Schema(description = "Company logo URL")
    private String companyLogo;
    
    @Schema(description = "Company language")
    private Language companyLanguage;
}
