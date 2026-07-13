package com.grash.dto;

import com.grash.model.Currency;
import com.grash.model.abstracts.BasicInfos;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CustomerPatchDTO extends BasicInfos {
    private String vendorType;

    private String description;

    private long rate;


    private String billingName;

    private String billingAddress;

    private String billingAddress2;

    private Currency billingCurrency;
}
