package com.grash.dto.requestPortal;

import com.grash.model.RequestPortalField;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "DTO for creating a new request portal")
public class RequestPortalPostDTO {
    @Schema(description = "Title of the request portal")
    @NotBlank
    private String title;
    
    @Schema(description = "Welcome message displayed on the portal")
    private String welcomeMessage;
    
    @Schema(description = "List of custom fields in the portal")
    private List<RequestPortalField> fields;
    
    @Schema(description = "reCAPTCHA token for verification")
    private String recaptchaToken;
}
