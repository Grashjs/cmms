package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.abstracts.BasicInfos;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class Customer extends BasicInfos {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String customerType;

    private String description;

    private long rate;


    private String billingName;

    private String billingAddress;

    private String billingAddress2;

    @OneToOne(fetch = FetchType.LAZY)
    private Currency billingCurrency;

    private boolean isDemo;

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Part_customer_Associations",
            joinColumns = @JoinColumn(name = "id_customer"),
            inverseJoinColumns = @JoinColumn(name = "id_part"),
            indexes = {
                    @Index(name = "idx_customer_part_customer_id", columnList = "id_customer"),
                    @Index(name = "idx_customer_part_part_id", columnList = "id_part")
            })
    private List<Part> parts = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Location_Customer_Associations",
            joinColumns = @JoinColumn(name = "id_customer"),
            inverseJoinColumns = @JoinColumn(name = "id_location"),
            indexes = {
                    @Index(name = "idx_customer_location_customer_id", columnList = "id_customer"),
                    @Index(name = "idx_customer_location_location_id", columnList = "id_location")
            })
    private List<Location> locations = new ArrayList<>();

    @ManyToMany
    @JsonIgnore
    @JoinTable(name = "T_Asset_Customer_Associations",
            joinColumns = @JoinColumn(name = "id_customer"),
            inverseJoinColumns = @JoinColumn(name = "id_asset"),
            indexes = {
                    @Index(name = "idx_customer_asset_customer_id", columnList = "id_customer"),
                    @Index(name = "idx_customer_asset_asset_id", columnList = "id_asset")
            })
    private List<Asset> assets = new ArrayList<>();

}
