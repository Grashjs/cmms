package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "Mini DTO for displaying basic request information")
public class RequestMiniDTO extends AuditShowDTO {

    @Schema(description = "Title of the request")
    private String title;

    @Schema(description = "Custom identifier for the request")
    private String customId;
}
