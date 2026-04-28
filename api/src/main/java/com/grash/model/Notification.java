package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Notification entity for user alerts and messages")
public class Notification extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Notification message", requiredMode = Schema.RequiredMode.REQUIRED)
    private String message;

    @Schema(description = "Whether the notification has been seen")
    private boolean seen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private User user;

    @Schema(description = "Type of notification")
    private NotificationType notificationType;

    @Schema(description = "ID of the related resource")
    private Long resourceId;


    public Notification(String message, User user, NotificationType notificationType, Long resourceId) {
        this.message = message;
        this.user = user;
        this.notificationType = notificationType;
        this.resourceId = resourceId;
    }

}


