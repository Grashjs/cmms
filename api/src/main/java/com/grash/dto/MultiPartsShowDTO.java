package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class MultiPartsShowDTO extends AuditShowDTO {

    private String name;

    private Collection<PartMiniDTO> parts;
}
