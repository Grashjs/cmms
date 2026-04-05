package com.grash.dto;

import com.grash.model.CompanySettings;
import com.grash.model.Subscription;
import lombok.Data;

import java.util.Map;

@Data
public class CompanyShowDTO extends AuditShowDTO {
    private String name;
    private String address;
    private String phone;
    private String website;
    private String email;

    private int employeesCount;

    private FileShowDTO logo;

    private String city;

    private String state;

    private String zipCode;

    private Subscription subscription;

    private CompanySettings companySettings;

    private boolean demo;

    /** feature name → override value (null means no override, use plan default) */
    private Map<String, Boolean> featureOverrides;
}
