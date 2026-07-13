package com.grash.dto;

import com.grash.model.SubscriptionPlan;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SubscriptionPatchDTO {

    private int usersCount;

    private boolean monthly;

    private SubscriptionPlan subscriptionPlan;
}
