package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.IdDTO;
import com.grash.model.abstracts.CompanyAudit;
import com.grash.model.enums.AssetStatus;
import com.grash.utils.Helper;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.BatchSize;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Asset entity representing equipment, machinery, or property in the CMMS system")
public class Asset extends CompanyAudit {

    @Schema(description = "Custom identifier for the asset", accessMode = Schema.AccessMode.READ_ONLY)
    private String customId;

    @Schema(description = "Indicates whether the asset is archived")
    private boolean archived;

    @Schema(description = "Image file associated with the asset", implementation = IdDTO.class)
    @OneToOne(fetch = FetchType.LAZY)
    private File image;

    @Schema(description = "The location where the asset is situated", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    @Schema(description = "The parent asset in a hierarchical structure", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asset parentAsset;

    @Schema(description = "The area or zone where the asset is located")
    private String area;

    @Schema(description = "Detailed description of the asset", maxLength = 10000)
    private String description;

    @Schema(description = "Barcode identifier for the asset")
    private String barCode;

    @Schema(description = "The category of the asset", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    private AssetCategory category;

    @Schema(description = "The name of the asset", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = "The primary user responsible for the asset", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    private User primaryUser;

    @Schema(description = "The acquisition cost of the asset")
    private Double acquisitionCost;

    @Schema(description = "NFC (Near Field Communication) identifier for the asset")
    private String nfcId;

    @Schema(description = "Indicates whether this is a demo asset")
    private boolean isDemo;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Asset_User_Associations",
            joinColumns = @JoinColumn(name = "id_asset"),
            inverseJoinColumns = @JoinColumn(name = "id_user"),
            indexes = {
                    @Index(name = "idx_asset_user_asset_id", columnList = "id_asset"),
                    @Index(name = "idx_asset_user_user_id", columnList = "id_user")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of users assigned to the asset", writeOnly = true)
    )
    private List<User> assignedTo = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Asset_Team_Associations",
            joinColumns = @JoinColumn(name = "id_asset"),
            inverseJoinColumns = @JoinColumn(name = "id_team"),
            indexes = {
                    @Index(name = "idx_asset_team_asset_id", columnList = "id_asset"),
                    @Index(name = "idx_asset_team_team_id", columnList = "id_team")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of teams assigned to the asset", writeOnly = true)
    )
    private List<Team> teams = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Asset_Vendor_Associations",
            joinColumns = @JoinColumn(name = "id_asset"),
            inverseJoinColumns = @JoinColumn(name = "id_vendor"),
            indexes = {
                    @Index(name = "idx_asset_vendor_asset_id", columnList = "id_asset"),
                    @Index(name = "idx_asset_vendor_vendor_id", columnList = "id_vendor")
            })
    @BatchSize(size = 64)
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of vendors associated with the asset", writeOnly = true)
    )
    private List<Vendor> vendors = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Asset_Customer_Associations",
            joinColumns = @JoinColumn(name = "id_asset"),
            inverseJoinColumns = @JoinColumn(name = "id_customer"),
            indexes = {
                    @Index(name = "idx_asset_customer_asset_id", columnList = "id_asset"),
                    @Index(name = "idx_asset_customer_customer_id", columnList = "id_customer")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of customers associated with the asset", writeOnly = true)
    )
    private List<Customer> customers = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Asset_Part_Associations",
            joinColumns = @JoinColumn(name = "id_asset"),
            inverseJoinColumns = @JoinColumn(name = "id_part"),
            indexes = {
                    @Index(name = "idx_asset_part_asset_id", columnList = "id_asset"),
                    @Index(name = "idx_asset_part_part_id", columnList = "id_part")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of parts associated with the asset", writeOnly = true)
    )
    private List<Part> parts = new ArrayList<>();

    @Schema(description = "Depreciation configuration for the asset", implementation = IdDTO.class)
    @OneToOne(fetch = FetchType.LAZY)
    private Deprecation deprecation;

    @Schema(description = "The warranty expiration date for the asset")
    private Date warrantyExpirationDate;

    @Schema(description = "The date when the asset was placed into service")
    private Date inServiceDate;

    @Schema(description = "Additional information about the asset")
    private String additionalInfos;

    @Schema(description = "The serial number of the asset")
    private String serialNumber;

    @Schema(description = "The model of the asset")
    private String model;

    @Schema(description = "The current status of the asset")
    private AssetStatus status = AssetStatus.OPERATIONAL;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Asset_File_Associations",
            joinColumns = @JoinColumn(name = "id_asset"),
            inverseJoinColumns = @JoinColumn(name = "id_file"),
            indexes = {
                    @Index(name = "idx_asset_file_asset_id", columnList = "id_asset"),
                    @Index(name = "idx_asset_file_file_id", columnList = "id_file")
            })
    @BatchSize(size = 64)
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of files attached to the asset", writeOnly = true)
    )
    private List<File> files = new ArrayList<>();

    @Schema(description = "The power rating or specification of the asset")
    private String power;

    @Schema(description = "The manufacturer of the asset")
    private String manufacturer;

    @OneToMany(mappedBy = "asset", cascade = CascadeType.ALL, orphanRemoval = true)
    @ArraySchema(schema = @Schema(implementation = CustomFieldValue.class))
    private List<CustomFieldValue> customFieldValues = new ArrayList<>();

    public Collection<User> getUsers() {
        Collection<User> users = new ArrayList<>();
        if (this.getPrimaryUser() != null) {
            users.add(this.getPrimaryUser());
        }
        if (this.getTeams() != null) {
            Collection<User> teamsUsers = new ArrayList<>();
            this.getTeams().forEach(team -> teamsUsers.addAll(team.getUsers()));
            users.addAll(teamsUsers);
        }
        if (this.getAssignedTo() != null) {
            users.addAll(this.getAssignedTo());
        }
        return users.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(User::getId))),
                ArrayList::new));
    }

    public List<User> getNewUsersToNotify(Collection<User> newUsers) {
        Collection<User> oldUsers = getUsers();
        return newUsers.stream().filter(newUser -> oldUsers.stream().noneMatch(user -> user.getId().equals(newUser.getId()))).
                collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(User::getId))),
                        ArrayList::new));
    }

    @JsonIgnore
    public long getAge() {
        return Helper.getDateDiff(getRealCreatedAt(), new Date(), TimeUnit.SECONDS);
    }

    @JsonIgnore
    public Date getRealCreatedAt() {
        return this.inServiceDate == null ? this.getCreatedAt() : this.inServiceDate;
    }
}





