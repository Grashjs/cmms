package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.CompanyAudit;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.http.HttpStatus;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Meter extends CompanyAudit {

    @NotNull
    private String name;

    private String unit;

    @NotNull
    private int updateFrequency;

    @ManyToOne(fetch = FetchType.LAZY)
    private MeterCategory meterCategory;

    @OneToOne(fetch = FetchType.LAZY)
    private File image;

    private boolean isDemo;

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_Meter_User_Associations",
            joinColumns = @JoinColumn(name = "id_meter"),
            inverseJoinColumns = @JoinColumn(name = "id_user"),
            indexes = {
                    @Index(name = "idx_meter_user_meter_id", columnList = "id_meter"),
                    @Index(name = "idx_meter_user_user_id", columnList = "id_user")
            })
    private List<OwnUser> users = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Asset asset;

    public void setUpdateFrequency(int updateFrequency) {
        if (updateFrequency < 1)
            throw new CustomException("Frequency should not be less than 1", HttpStatus.NOT_ACCEPTABLE);
        this.updateFrequency = updateFrequency;
    }
}
