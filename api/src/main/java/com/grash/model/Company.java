package com.grash.model;

import com.grash.model.abstracts.Audit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@NoArgsConstructor
@EqualsAndHashCode(exclude = "companySettings", callSuper = false)
@Schema(description = "Company entity representing an organization in the CMMS system")
public class Company extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Company name")
    private String name;
    @Schema(description = "Company address")
    private String address;
    @Schema(description = "Company phone number")
    private String phone;
    @Schema(description = "Company website")
    private String website;
    @Schema(description = "Company email address")
    private String email;

    @Schema(description = "Number of employees")
    private int employeesCount;

    @OneToOne(fetch = FetchType.LAZY)
    private File logo;

    @Schema(description = "Company city")
    private String city;

    @Schema(description = "Company state")
    private String state;

    @Schema(description = "Company zip code")
    private String zipCode;

    @OneToOne(fetch = FetchType.LAZY)
    private Subscription subscription;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private CompanySettings companySettings = new CompanySettings(this);

    @Schema(description = "Indicates whether this is a demo company")
    private boolean demo;

    @Schema(description = "Whether the first work order has been created")
    private boolean firstWorkOrderCreated = false;

    @Schema(description = "Whether users have been invited")
    private boolean invitedUsers = false;

    @Schema(description = "Whether assets have been imported")
    private boolean importedAssets = false;

    public Company(String companyName, int employeesCount, Subscription subscription) {
        this.name = companyName;
        this.employeesCount = employeesCount;
        this.subscription = subscription;

    }
}

