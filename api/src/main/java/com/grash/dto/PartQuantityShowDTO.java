package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PartQuantityShowDTO extends AuditShowDTO {

    private double quantity;
    private PartMiniDTO part;
}
