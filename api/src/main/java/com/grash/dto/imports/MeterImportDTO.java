package com.grash.dto.imports;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class MeterImportDTO {

    private Long id;
    
    private String name;

    private String unit;

    private int updateFrequency;

    private String meterCategory;

    private String locationName;

    private Collection<String> usersEmails;
}
