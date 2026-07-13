package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class DeprecationPatchDTO {

    private long purchasePrice;

    private Date purchaseDate;

    private String residualValue;

    private String usefulLIfe;

    private int rate;

    private long currentValue;
}
