package com.grash.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import com.grash.utils.Helper;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
public class CompanySettings {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL)
    private GeneralPreferences generalPreferences = new GeneralPreferences(this);

    @OneToOne(cascade = CascadeType.ALL)
    private WorkOrderConfiguration workOrderConfiguration = new WorkOrderConfiguration(this);

    @OneToOne(cascade = CascadeType.ALL)
    private WorkOrderRequestConfiguration WorkOrderRequestConfiguration = new WorkOrderRequestConfiguration(this);

    @OneToOne(cascade = CascadeType.ALL, mappedBy = "companySettings")
    private UiConfiguration uiConfiguration = new UiConfiguration(this);
    @OneToOne
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private Company company;

//    @OneToOne
//   private AssetFieldsConfiguration assetFieldsConfiguration;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "companySettings", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Role> roleList = getDefaultRoles();

    public CompanySettings(Company company) {
        this.company = company;
    }

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "companySettings", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<CostCategory> costCategories = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "companySettings", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<TimeCategory> timeCategories = new ArrayList<>();


    private List<CostCategory> createCostCategories(List<String> costCategories) {
        return costCategories.stream().map(costCategory -> new CostCategory(costCategory, this)).collect(Collectors.toList());
    }

    private List<TimeCategory> createTimeCategories(List<String> timeCategories) {
        return timeCategories.stream().map(timeCategory -> new TimeCategory(timeCategory, this)).collect(Collectors.toList());
    }

    private Set<Role> getDefaultRoles() {
        return Helper.getDefaultRoles().stream().peek(role -> {
            role.setCompanySettings(this);
            role.setRoleType(RoleType.ROLE_CLIENT);
        }).collect(Collectors.toSet());
    }
}
