package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.IdDTO;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Location entity representing a physical place, facility, or area in the CMMS system")
public class Location extends CompanyAudit {
    @Schema(description = "Custom identifier for the location", accessMode = Schema.AccessMode.READ_ONLY)
    private String customId;

    @Schema(description = "The name of the location", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = "The physical address of the location")
    private String address;

    @Schema(description = "The longitude coordinate of the location")
    private Double longitude;

    @Schema(description = "The latitude coordinate of the location")
    private Double latitude;

    @Schema(description = "Indicates whether this is a demo location")
    private boolean isDemo;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Location_User_Associations",
            joinColumns = @JoinColumn(name = "id_location"),
            inverseJoinColumns = @JoinColumn(name = "id_user"),
            indexes = {
                    @Index(name = "idx_location_worker_location_id", columnList = "id_location"),
                    @Index(name = "idx_location_worker_worker_id", columnList = "id_user")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of workers assigned to the location", writeOnly = true)
    )
    private List<User> workers = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Location_Team_Associations",
            joinColumns = @JoinColumn(name = "id_location"),
            inverseJoinColumns = @JoinColumn(name = "id_team"),
            indexes = {
                    @Index(name = "idx_location_team_location_id", columnList = "id_location"),
                    @Index(name = "idx_location_team_team_id", columnList = "id_team")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of teams assigned to the location", writeOnly = true)
    )
    private List<Team> teams = new ArrayList<>();

    @Schema(description = "The parent location in a hierarchical structure", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Location parentLocation;

    @Schema(description = "Image file associated with the location", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    private File image;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Location_Vendor_Associations",
            joinColumns = @JoinColumn(name = "id_location"),
            inverseJoinColumns = @JoinColumn(name = "id_vendor"),
            indexes = {
                    @Index(name = "idx_location_vendor_location_id", columnList = "id_location"),
                    @Index(name = "idx_location_vendor_vendor_id", columnList = "id_vendor")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of vendors associated with the location", writeOnly = true)
    )
    private List<Vendor> vendors = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Location_Customer_Associations",
            joinColumns = @JoinColumn(name = "id_location"),
            inverseJoinColumns = @JoinColumn(name = "id_customer"),
            indexes = {
                    @Index(name = "idx_location_customer_location_id", columnList = "id_location"),
                    @Index(name = "idx_location_customer_customer_id", columnList = "id_customer")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of customers associated with the location", writeOnly = true)
    )
    private List<Customer> customers = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Location_File_Associations",
            joinColumns = @JoinColumn(name = "id_location"),
            inverseJoinColumns = @JoinColumn(name = "id_file"),
            indexes = {
                    @Index(name = "idx_location_file_location_id", columnList = "id_location"),
                    @Index(name = "idx_location_file_file_id", columnList = "id_file")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of files attached to the location", writeOnly = true)
    )
    private List<File> files = new ArrayList<>();

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    @ArraySchema(schema = @Schema(implementation = CustomFieldValue.class))
    private List<CustomFieldValue> customFieldValues = new ArrayList<>();

    public Collection<User> getUsers() {
        Collection<User> users = new ArrayList<>();
        if (this.getTeams() != null) {
            Collection<User> teamsUsers = new ArrayList<>();
            this.getTeams().forEach(team -> teamsUsers.addAll(team.getUsers()));
            users.addAll(teamsUsers);
        }
        if (this.getWorkers() != null) {
            users.addAll(this.getWorkers());
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
}



