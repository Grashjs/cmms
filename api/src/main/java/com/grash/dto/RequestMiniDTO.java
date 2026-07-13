package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RequestMiniDTO extends AuditShowDTO {

    private String title;

    private String customId;
}
