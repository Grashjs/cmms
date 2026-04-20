package com.grash.dto;

import com.grash.model.File;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO representing a relationship between super account and child company")
public class SuperAccountRelationDTO {
    @Schema(description = "Child company name")
    private String childCompanyName;
    
    @Schema(description = "Child user ID")
    private Long childUserId;
    
    @Schema(description = "Super user ID")
    private Long superUserId;
}
