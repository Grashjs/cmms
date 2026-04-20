package com.grash.dto.fastSpring.payloads;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "FastSpring subscription resume payload")
public class ResumePayload {
    @Schema(description = "List of resumed subscriptions")
    private List<SingleSubscription> subscriptions = new ArrayList<>();

    public ResumePayload(String subscription, String deactivation) {
        SingleSubscription singleSubscription = new SingleSubscription();
        singleSubscription.setSubscription(subscription);
        singleSubscription.setDeactivation(deactivation);
        this.subscriptions.add(singleSubscription);
    }

    @Data
    @Schema(description = "Single subscription resume entry")
    private static class SingleSubscription {
        @Schema(description = "Subscription reference to resume")
        private String subscription;
        @Schema(description = "Deactivation reference to reverse")
        private String deactivation;
    }
}
