package com.grash.dto.apiKey;

import com.grash.dto.UserMiniDTO;
import com.grash.dto.AuditShowDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Data
@Schema(description = "DTO for displaying API key details in API responses")
public class ApiKeyShowDTO extends AuditShowDTO {
    @Schema(description = "API key label")
    private String label;
    
    @Schema(description = "API key code")
    private String code;
    
    @Schema(description = "User who owns the API key")
    private UserMiniDTO user;
    
    @Schema(description = "Last used timestamp")
    private Date lastUsed;
}
