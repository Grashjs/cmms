package com.grash.dto.requestPortal;

import com.grash.dto.AuditShowDTO;
import lombok.Data;

@Data
public class RequestPortalMiniDTO extends AuditShowDTO {
    private String title;
    private String uuid;
}