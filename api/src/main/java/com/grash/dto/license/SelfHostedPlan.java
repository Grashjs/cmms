package com.grash.dto.license;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SelfHostedPlan {
    private String id;
    private BigDecimal pricePerUser;
    private String name;
    private boolean monthly;
    private String keygenPolicyId;
}