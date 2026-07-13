package com.grash.dto;

import com.grash.model.File;
import lombok.Data;

@Data
public class SuperAccountRelationDTO {
    private String childCompanyName;
    private Long childUserId;
    private Long superUserId;
}
