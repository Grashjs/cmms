package com.grash.dto.apiKey;

import com.grash.dto.UserMiniDTO;
import com.grash.model.OwnUser;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.grash.dto.AuditShowDTO;

import java.util.Date;

@Data
public class ApiKeyShowDTO extends AuditShowDTO {
    private String label;
    private String code;
    private UserMiniDTO user;
    private Date lastUsed;
}
