package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.Audit;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.PlanFeatures;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class OwnUser extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String firstName;

    @NotNull
    private String lastName;

    private long rate;

    @OneToOne(fetch = FetchType.LAZY)
    private File image;

    @Column(unique = true)
    @NotNull
    private String email;

    private String phone;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private Role role;

    private String jobTitle;

    @NotNull
    private String username;

    @JsonIgnore
    @NotNull
    private String password;

    private Date lastLogin;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean enabled;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean enabledInSubscription = true;

    @ManyToOne(fetch = FetchType.LAZY)
    private Company company;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean ownsCompany;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private UserSettings userSettings = new UserSettings();

    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Asset_User_Associations",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_asset"),
            indexes = {
                    @Index(name = "idx_user_asset_user_id", columnList = "id_user"),
                    @Index(name = "idx_user_asset_asset_id", columnList = "id_asset")
            })
    private List<Asset> asset = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Location_User_Associations",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_location"),
            indexes = {
                    @Index(name = "idx_user_location_user_id", columnList = "id_user"),
                    @Index(name = "idx_user_location_location_id", columnList = "id_location")
            })
    private List<Location> locations = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Meter_User_Associations",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_meter"),
            indexes = {
                    @Index(name = "idx_user_meter_user_id", columnList = "id_user"),
                    @Index(name = "idx_user_meter_meter_id", columnList = "id_meter")
            })
    private List<Meter> meters = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Part_User_Associations",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_part"),
            indexes = {
                    @Index(name = "idx_user_part_user_id", columnList = "id_user"),
                    @Index(name = "idx_user_part_part_id", columnList = "id_part")
            })
    private List<Part> parts = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Team_User_Associations",
            joinColumns = @JoinColumn(name = "id_user"),
            inverseJoinColumns = @JoinColumn(name = "id_team"),
            indexes = {
                    @Index(name = "idx_user_team_user_id", columnList = "id_user"),
                    @Index(name = "idx_user_team_team_id", columnList = "id_team")
            })
    private List<Team> teams = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    private List<PreventiveMaintenance> preventiveMaintenances = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    private List<WorkOrder> workOrders = new ArrayList<>();

    @OneToMany(mappedBy = "superUser", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<SuperAccountRelation> superAccountRelations = new ArrayList<>();

    @OneToOne(mappedBy = "childUser")
    @JsonIgnore
    private SuperAccountRelation parentSuperAccount;

    // SSO fields
    private String ssoProvider;
    private String ssoProviderId;
    private boolean createdViaSso = false;

    private String utmSource;
    private String utmMedium;
    private String utmCampaign;
    private String utmTerm;
    private String utmContent;
    private String gclid;
    private String fbclid;
    private String referrer;


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

}

