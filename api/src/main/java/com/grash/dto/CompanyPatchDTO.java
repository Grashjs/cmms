package com.grash.dto;

import com.grash.model.File;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(implementation = IdDTO.class)
    private File logo;
    private String city;
    private String state;
    private String zipCode;
}
