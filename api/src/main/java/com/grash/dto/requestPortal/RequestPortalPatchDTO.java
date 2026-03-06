package com.grash.dto.requestPortal;

import com.grash.model.RequestPortalField;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RequestPortalPatchDTO {
    @NotBlank
    private String title;
    private String welcomeMessage;
    private List<RequestPortalField> fields;
}
