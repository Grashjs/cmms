package com.grash.model;

import com.grash.model.enums.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Data
@Schema(description = "User settings for notification and display preferences")
public class UserSettings {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Whether email notifications are enabled")
    private boolean emailNotified = true;
    @Schema(description = "Whether email updates for work orders are enabled")
    private boolean emailUpdatesForWorkOrders = true;
    @Schema(description = "Whether email updates for requests are enabled")
    private boolean emailUpdatesForRequests = true;
    @Schema(description = "Whether email updates for purchase orders are enabled")
    private boolean emailUpdatesForPurchaseOrders = true;
    @Schema(description = "Whether stats for assigned work orders are shown")
    private boolean statsForAssignedWorkOrders = true;

    @Nullable
    private Language language;

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

