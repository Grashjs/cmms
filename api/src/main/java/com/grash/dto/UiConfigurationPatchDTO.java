package com.grash.dto;

import lombok.Data;

@Data
public class UiConfigurationPatchDTO {

    private boolean requests;
    private boolean locations;
    private boolean meters;
    private boolean vendorsAndCustomers;
}
