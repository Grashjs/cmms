package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class LicenseRelationships {
    private RelationshipObject account;
    private RelationshipObject product;
    private RelationshipObject policy;
    private RelationshipObject group;
    private RelationshipObject owner;
    private RelationshipWithMeta users;
    private RelationshipWithMeta machines;
    private RelationshipLinks tokens;
    private RelationshipLinks entitlements;
}
