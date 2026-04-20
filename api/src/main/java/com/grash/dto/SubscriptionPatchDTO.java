package com.grash.dto;

import com.grash.model.SubscriptionPlan;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing subscription")
public class SubscriptionPatchDTO {

    @Schema(description = "Number of users")
    private int usersCount;

    @Schema(description = "Whether the subscription is monthly")
    private boolean monthly;

    @Schema(description = "Subscription plan reference", implementation = IdDTO.class)
    private SubscriptionPlan subscriptionPlan;
}
