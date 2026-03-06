package com.grash.dto.requestPortal;

import com.grash.model.RequestPortalField;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RequestPortalPostDTO {
    @NotBlank
    private String title;
    private String welcomeMessage;
    private List<RequestPortalField> fields;
    private String recaptchaToken;
}
