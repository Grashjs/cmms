package com.grash.dto.fastSpring.payloads;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ResumePayload {
    private List<SingleSubscription> subscriptions = new ArrayList<>();

    public ResumePayload(String subscription, String deactivation) {
        SingleSubscription singleSubscription = new SingleSubscription();
        singleSubscription.setSubscription(subscription);
        singleSubscription.setDeactivation(deactivation);
        this.subscriptions.add(singleSubscription);
    }

    @Data
    private static class SingleSubscription {
        private String subscription;
        private String deactivation;
    }
}
