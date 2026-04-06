package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "License relationships linking to account, product, users, machines, and entitlements")
public class LicenseRelationships {
    @Schema(description = "Account relationship for the license owner")
    private RelationshipObject account;
    @Schema(description = "Product relationship defining the licensed product")
    private RelationshipObject product;
    @Schema(description = "Policy relationship for license validation rules")
    private RelationshipObject policy;
    @Schema(description = "Group relationship for license organization")
    private RelationshipObject group;
    @Schema(description = "Owner relationship for the license")
    private RelationshipObject owner;
    @Schema(description = "Users associated with this license")
    private RelationshipWithMeta users;
    @Schema(description = "Machines associated with this license")
    private RelationshipWithMeta machines;
    @Schema(description = "Tokens associated with this license")
    private RelationshipLinks tokens;
    @Schema(description = "Entitlements granted by this license")
    private RelationshipLinks entitlements;
}
