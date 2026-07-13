package com.grash.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
public class UiConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private boolean requests = true;
    private boolean locations = true;
    private boolean meters = true;
    private boolean vendorsAndCustomers = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JsonIgnore
    private CompanySettings companySettings;

    public UiConfiguration(CompanySettings companySettings) {
        this.companySettings = companySettings;
    }
}
