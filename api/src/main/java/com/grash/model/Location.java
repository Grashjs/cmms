package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.CompanyAudit;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
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
public class Location extends CompanyAudit {
    private String customId;
    
    @NotNull
    private String name;

    private String address;

    private Double longitude;

    private Double latitude;

    private boolean hasChildren;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Location_User_Associations",
            joinColumns = @JoinColumn(name = "id_location"),
            inverseJoinColumns = @JoinColumn(name = "id_user"),
            indexes = {
                    @Index(name = "idx_location_worker_location_id", columnList = "id_location"),
                    @Index(name = "idx_location_worker_worker_id", columnList = "id_user")
            })
    private List<OwnUser> workers = new ArrayList<>();

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Location_Team_Associations",
            joinColumns = @JoinColumn(name = "id_location"),
            inverseJoinColumns = @JoinColumn(name = "id_team"),
            indexes = {
                    @Index(name = "idx_location_team_location_id", columnList = "id_location"),
                    @Index(name = "idx_location_team_team_id", columnList = "id_team")
            })
    private List<Team> teams = new ArrayList<>();

    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Location parentLocation;

    @ManyToOne
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
    private List<File> files = new ArrayList<>();

    public Collection<OwnUser> getUsers() {
        Collection<OwnUser> users = new ArrayList<>();
        if (this.getTeams() != null) {
            Collection<OwnUser> teamsUsers = new ArrayList<>();
            this.getTeams().forEach(team -> teamsUsers.addAll(team.getUsers()));
            users.addAll(teamsUsers);
        }
        if (this.getWorkers() != null) {
            users.addAll(this.getWorkers());
        }
        return users.stream().collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(OwnUser::getId))),
                ArrayList::new));
    }

    public List<OwnUser> getNewUsersToNotify(Collection<OwnUser> newUsers) {
        Collection<OwnUser> oldUsers = getUsers();
        return newUsers.stream().filter(newUser -> oldUsers.stream().noneMatch(user -> user.getId().equals(newUser.getId()))).
                collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparingLong(OwnUser::getId))),
                        ArrayList::new));
    }
}

