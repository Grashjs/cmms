package com.grash.dto.requestPortal;

import com.grash.model.RequestPortalField;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.grash.dto.AuditShowDTO;

import java.util.List;

@Data
public class RequestPortalShowDTO extends AuditShowDTO {
    private String title;
    private String welcomeMessage;
    private String uuid;
    private List<RequestPortalField> fields;
}
