package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.NotificationType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
public class Notification extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String message;

    private boolean seen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private OwnUser user;

    private NotificationType notificationType;

    private Long resourceId;


    public Notification(String message, OwnUser user, NotificationType notificationType, Long resourceId) {
        this.message = message;
        this.user = user;
        this.notificationType = notificationType;
        this.resourceId = resourceId;
    }

}
