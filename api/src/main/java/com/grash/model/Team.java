package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.CompanyAudit;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Team extends CompanyAudit {
    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Team_User_Associations",
            joinColumns = @JoinColumn(name = "id_team"),
            inverseJoinColumns = @JoinColumn(name = "id_user"),
            indexes = {
                    @Index(name = "idx_team_user_team_id", columnList = "id_team"),
                    @Index(name = "idx_team_user_user_id", columnList = "id_user")
            })
    private List<OwnUser> users = new ArrayList<>();

    @NotNull
    private String name;

    private String description;

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Asset_Team_Associations",
            joinColumns = @JoinColumn(name = "id_team"),
            inverseJoinColumns = @JoinColumn(name = "id_asset"),
            indexes = {
                    @Index(name = "idx_team_asset_team_id", columnList = "id_team"),
                    @Index(name = "idx_team_asset_asset_id", columnList = "id_asset")
            })
    private List<Asset> asset = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Location_Team_Associations",
            joinColumns = @JoinColumn(name = "id_team"),
            inverseJoinColumns = @JoinColumn(name = "id_location"),
            indexes = {
                    @Index(name = "idx_team_location_team_id", columnList = "id_team"),
                    @Index(name = "idx_team_location_location_id", columnList = "id_location")
            })
    private List<Location> locations = new ArrayList<>();
}
