package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.IdDTO;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

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
@Schema(description = "Part entity representing a spare part, component, or supply item in the CMMS system")
public class Part extends CompanyAudit {
    @Schema(description = "The name of the part", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = "The cost of the part")
    private double cost;

    @Schema(description = "Indicates whether this is a demo part")
    private boolean isDemo;


    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Part_User_Associations",
            joinColumns = @JoinColumn(name = "id_part"),
            inverseJoinColumns = @JoinColumn(name = "id_user"),
            indexes = {
                    @Index(name = "idx_part_user_part_id", columnList = "id_part"),
                    @Index(name = "idx_part_user_user_id", columnList = "id_user")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of users assigned to the part", writeOnly = true)
    )
    private List<User> assignedTo = new ArrayList<>();

    @Schema(description = "Barcode identifier for the part")
    private String barcode;

    @Schema(description = "Detailed description of the part")
    private String description;

    @Schema(description = "The category of the part", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    private PartCategory category;

    @Schema(description = "The current quantity of the part in stock")
    private double quantity;

    @Schema(description = "The area or storage location where the part is kept")
    private String area;

    @Schema(description = "Additional information about the part")
    private String additionalInfos;

    @Schema(description = "Indicates whether this is a non-stock part")
    private boolean nonStock;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Part_File_Associations",
            joinColumns = @JoinColumn(name = "id_part"),
            inverseJoinColumns = @JoinColumn(name = "id_file"),
            indexes = {
                    @Index(name = "idx_part_file_part_id", columnList = "id_part"),
                    @Index(name = "idx_part_file_file_id", columnList = "id_file")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of files attached to the part", writeOnly = true)
    )
    private List<File> files = new ArrayList<>();

    @Schema(description = "Image file associated with the part", implementation = IdDTO.class)
    @OneToOne(fetch = FetchType.LAZY)
    private File image;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Part_Customer_Associations",
            joinColumns = @JoinColumn(name = "id_part"),
            inverseJoinColumns = @JoinColumn(name = "id_customer"),
            indexes = {
                    @Index(name = "idx_part_customer_part_id", columnList = "id_part"),
                    @Index(name = "idx_part_customer_customer_id", columnList = "id_customer")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of customers associated with the part", writeOnly = true)
    )
    private List<Customer> customers = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Part_Vendor_Associations",
            joinColumns = @JoinColumn(name = "id_part"),
            inverseJoinColumns = @JoinColumn(name = "id_vendor"),
            indexes = {
                    @Index(name = "idx_part_vendor_part_id", columnList = "id_part"),
                    @Index(name = "idx_part_vendor_vendor_id", columnList = "id_vendor")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of vendors associated with the part", writeOnly = true)
    )
    private List<Vendor> vendors = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    private List<PreventiveMaintenance> preventiveMaintenances = new ArrayList<>();

    @Schema(description = "The minimum quantity threshold for the part")
    private double minQuantity;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Part_Team_Associations",
            joinColumns = @JoinColumn(name = "id_part"),
            inverseJoinColumns = @JoinColumn(name = "id_team"),
            indexes = {
                    @Index(name = "idx_part_team_part_id", columnList = "id_part"),
                    @Index(name = "idx_part_team_team_id", columnList = "id_team")
            })
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of teams assigned to the part", writeOnly = true)
    )
    private List<Team> teams = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Asset_Part_Associations",
            joinColumns = @JoinColumn(name = "id_part"),
            inverseJoinColumns = @JoinColumn(name = "id_asset"),
            indexes = {
                    @Index(name = "idx_part_asset_part_id", columnList = "id_part"),
                    @Index(name = "idx_part_asset_asset_id", columnList = "id_asset")
            })
    private List<Asset> assets = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_MultiParts_Part_Associations",
            joinColumns = @JoinColumn(name = "id_part"),
            inverseJoinColumns = @JoinColumn(name = "id_multi_parts"),
            indexes = {
                    @Index(name = "idx_part_multi_parts_part_id", columnList = "id_part"),
                    @Index(name = "idx_part_multi_parts_multi_parts_id", columnList = "id_multi_parts")
            })
    private List<MultiParts> multiParts = new ArrayList<>();

    @Schema(description = "The unit of measurement for the part")
    private String unit;

    @JsonIgnore
    public Collection<User> getUsers() {
        Collection<User> users = new ArrayList<>();

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

    @JsonIgnore
    public List<User> getNewUsersToNotify(Collection<User> newUsers) {
        Collection<User> oldUsers = getUsers();
        return newUsers.stream().filter(newUser -> oldUsers.stream().noneMatch(user -> user.getId().equals(newUser.getId()))).
                collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(User::getId))),
                        ArrayList::new));
    }

    //TODO
    //Location

    public void setQuantity(double quantity) {
        if (quantity < 0) throw new CustomException("The quantity should not be negative", HttpStatus.NOT_ACCEPTABLE);
        this.quantity = quantity;
    }

    public void setMinQuantity(double minQuantity) {
        if (minQuantity < 0)
            throw new CustomException("The quantity should not be negative", HttpStatus.NOT_ACCEPTABLE);
        this.minQuantity = minQuantity;
    }

}


