package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "own_user")
@Schema(description = "User entity representing a user account in the CMMS system")
public class User extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "First name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String firstName;

    @NotNull
    @Schema(description = "Last name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String lastName;

    @Schema(description = "User hourly rate")
    private long rate;

    @OneToOne(fetch = FetchType.LAZY)
    private File image;

    @Column(unique = true)
    @NotNull
    @Schema(description = "Email address", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @Schema(description = "Phone number")
    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private Role role;

    @Schema(description = "Job title")
    private String jobTitle;

    @NotNull
    @Schema(description = "Username", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @JsonIgnore
    @NotNull
    @Schema(description = "Password", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Schema(description = "Last login date", accessMode = Schema.AccessMode.READ_ONLY)
    private Date lastLogin;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Whether the user account is enabled", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean enabled;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Whether the user is enabled in the subscription", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean enabledInSubscription = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Whether the user owns the company", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean ownsCompany;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSettings userSettings = new UserSettings();

    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    @OneToMany(mappedBy = "superUser", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SuperAccountRelation> superAccountRelations = new ArrayList<>();

    @OneToOne(mappedBy = "childUser")
    @JsonIgnore
    private SuperAccountRelation parentSuperAccount;

    // SSO fields
    @Schema(description = "SSO provider name")
    private String ssoProvider;
    @Schema(description = "SSO provider user ID")
    private String ssoProviderId;
    @Schema(description = "Whether the account was created via SSO")
    private boolean createdViaSso = false;

    private String ldapId;

    @Schema(description = "UTM source for tracking")
    private String utmSource;
    @Schema(description = "UTM medium for tracking")
    private String utmMedium;
    @Schema(description = "UTM campaign for tracking")
    private String utmCampaign;
    @Schema(description = "UTM term for tracking")
    private String utmTerm;
    @Schema(description = "UTM content for tracking")
    private String utmContent;
    @Schema(description = "Google Click ID")
    private String gclid;
    @Schema(description = "Facebook Click ID")
    private String fbclid;
    @Schema(description = "Referrer URL")
    private String referrer;

    @Schema(description = "Paddle user ID")
    private String paddleUserId;


    public int hashCode() {
        return Math.toIntExact(id);
    }

    public boolean canSeeAnalytics() {
        return this.getRole().getViewPermissions().contains(PermissionEntity.ANALYTICS) && this.getCompany().getSubscription().getSubscriptionPlan().getFeatures().contains(PlanFeatures.ANALYTICS);
    }

    public String getFullName() {
        return this.firstName + " " + this.lastName;
    }

    @JsonIgnore
    public boolean isEnabledInSubscriptionAndPaid() {
        return enabledInSubscription && this.getRole().isPaid();
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim().toLowerCase();
    }
}



