package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.dto.IdDTO;
import com.grash.exception.CustomException;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.springframework.http.HttpStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Meter entity representing a measurement device or gauge for tracking readings in the CMMS " +
        "system")
public class Meter extends CompanyAudit {

    @Schema(description = "The name of the meter", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = "The unit of measurement for the meter (e.g., kWh, gallons, hours)")
    private String unit;

    @Schema(description = "The frequency at which the meter readings should be updated")
    @NotNull
    private int updateFrequency;

    @Schema(description = "The category of the meter", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    private MeterCategory meterCategory;

    @Schema(description = "Image file associated with the meter", implementation = IdDTO.class)
    @OneToOne(fetch = FetchType.LAZY)
    private File image;

    @Schema(description = "Indicates whether this is a demo meter")
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
    @ArraySchema(
            schema = @Schema(implementation = IdDTO.class),
            arraySchema = @Schema(description = "List of users who have access to the meter", writeOnly = true)
    )
    private List<User> users = new ArrayList<>();

    @Schema(description = "The location where the meter is installed", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    private Location location;

    @Schema(description = "The asset on which the meter is installed", implementation = IdDTO.class)
    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Asset asset;

    @OneToMany(mappedBy = "meter", cascade = CascadeType.ALL, orphanRemoval = true)
    @ArraySchema(schema = @Schema(implementation = CustomFieldValue.class))
    private List<CustomFieldValue> customFieldValues = new ArrayList<>();

    public void setUpdateFrequency(int updateFrequency) {
        if (updateFrequency < 1)
            throw new CustomException("Frequency should not be less than 1", HttpStatus.NOT_ACCEPTABLE);
        this.updateFrequency = updateFrequency;
    }
}


