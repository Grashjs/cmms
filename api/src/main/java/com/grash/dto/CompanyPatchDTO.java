package com.grash.dto;

import com.grash.model.File;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CompanyPatchDTO {

    private String name;
    private String address;
    private String phone;
    private String website;
    private String email;
    private File logo;
    private String city;
    private String state;
    private String zipCode;
}
