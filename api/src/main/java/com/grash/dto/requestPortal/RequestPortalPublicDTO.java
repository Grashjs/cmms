package com.grash.dto.requestPortal;

import com.grash.dto.AuditShowDTO;
import com.grash.model.RequestPortalField;
import lombok.Data;

import java.util.List;

@Data
public class RequestPortalPublicDTO extends AuditShowDTO {
    private String title;
    private String welcomeMessage;
    private List<RequestPortalField> fields;
    private String uuid;
    private Long companyId;
    private String companyName;
    private String companyLogo;
}
