package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Location;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserPatchDTO {

    private String firstName;

    private String lastName;

    private long rate;

    private String phone;

    private String jobTitle;

    @Schema(implementation = IdDTO.class)
    private Location location;

    @Schema(implementation = IdDTO.class)
    private File image;

    private String newPassword;
}
