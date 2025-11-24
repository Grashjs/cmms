package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.Audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Date;

@Entity
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Subscription extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private int usersCount;

    private boolean monthly;

    private boolean cancelled;

    private boolean activated;

    @JsonIgnore
    private String fastSpringId;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private SubscriptionPlan subscriptionPlan;

    private Date startsOn;

    private Date endsOn;

    private boolean downgradeNeeded;

    private boolean upgradeNeeded;

    public void setUsersCount(int usersCount) {
        if (usersCount < 1) throw new CustomException("Users count should not be less than 1", HttpStatus.NOT_ACCEPTABLE);
        this.usersCount = usersCount;
    }
}
