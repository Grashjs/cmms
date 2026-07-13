package com.grash.dto.imports;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class LocationImportDTO {

    private Long id;
    private String name;

    private String address;

    private Double longitude;

    private Double latitude;

    private String parentLocationName;

    private Collection<String> workersEmails;

    private Collection<String> teamsNames;

    private Collection<String> customersNames;

    private Collection<String> vendorsNames;

}
