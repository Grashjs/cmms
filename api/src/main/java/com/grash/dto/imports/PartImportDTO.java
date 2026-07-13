package com.grash.dto.imports;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class PartImportDTO {

    private Long id;
    private String name;

    private double cost;

    private String category;

    private String nonStock;

    private String barcode;

    private String description;

    private double quantity;

    private String additionalInfos;

    private String area;

    private double minQuantity;

    private String locationName;

    private Collection<String> assignedToEmails;

    private Collection<String> teamsNames;
    private Collection<String> customersNames;
    private Collection<String> vendorsNames;
}
