package com.grash.dto;

import com.grash.model.abstracts.BasicInfos;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VendorPatchDTO extends BasicInfos {

    private String vendorType;

    private String description;

    private long rate;
}
