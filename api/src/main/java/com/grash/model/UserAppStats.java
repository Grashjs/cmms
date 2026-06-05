package com.grash.model;

import com.grash.model.abstracts.Audit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

@Entity
@Data
public class UserAppStats extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @Schema(description = "Number of completed work orders")
    private int completedWorkOrders = 0;

    @Schema(description = "Number of app sessions")
    private int appSessions = 0;

    @Schema(description = "Date of the last review prompt shown")
    private Date lastReviewPromptAt;

    @Schema(description = "Number of times review prompt was shown")
    private int reviewPromptShownCount = 0;

    @Schema(description = "Number of times user clicked review prompt")
    private int reviewClickCount = 0;

    @Schema(description = "Whether user has already rated the app")
    private boolean hasRatedApp = false;

    private String feedback;

    @OneToOne
    @NotNull
    private User user;

}