package com.grash.model;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
@NoArgsConstructor
@Data
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private boolean emailNotified = true;
    private boolean emailUpdatesForWorkOrders = true;
    private boolean emailUpdatesForRequests = true;
    private boolean emailUpdatesForPurchaseOrders = true;
    private boolean statsForAssignedWorkOrders = true;

    public boolean shouldEmailUpdatesForWorkOrders() {
        return emailNotified && emailUpdatesForWorkOrders;
    }

    public boolean shouldEmailUpdatesForRequests() {
        return emailNotified && emailUpdatesForRequests;
    }

    public boolean shouldEmailUpdatesForPurchaseOrders() {
        return emailNotified && emailUpdatesForPurchaseOrders;
    }

    public boolean shouldShowStatsForAssignedWorkOrders() {
        return emailNotified && statsForAssignedWorkOrders;
    }
}

