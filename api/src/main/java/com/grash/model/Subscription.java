package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.SubscriptionScheduledChangeType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Subscription entity for managing company subscription details")
public class Subscription extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Number of users allowed in subscription", requiredMode = Schema.RequiredMode.REQUIRED)
    private int usersCount;

    @Schema(description = "Whether the billing is monthly")
    private boolean monthly;

    @Schema(description = "Whether the subscription is activated")
    private boolean activated;

    @JsonIgnore
    private String paddleSubscriptionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private SubscriptionPlan subscriptionPlan;

    @Schema(description = "Subscription start date")
    private Date startsOn;

    @Schema(description = "Subscription end date")
    private Date endsOn;

    @Schema(description = "Whether a downgrade is needed")
    private boolean downgradeNeeded;

    @Schema(description = "Whether an upgrade is needed")
    private boolean upgradeNeeded;

    @Schema(description = "Date of scheduled change")
    private Date scheduledChangeDate;

    @Schema(description = "Type of scheduled change")
    private SubscriptionScheduledChangeType scheduledChangeType;

    public void setUsersCount(int usersCount) {
        if (usersCount < 1)
            throw new CustomException("Users count should not be less than 1", HttpStatus.NOT_ACCEPTABLE);
        this.usersCount = usersCount;
    }
}


