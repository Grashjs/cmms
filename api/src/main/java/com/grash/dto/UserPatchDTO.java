package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Location;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching user information")
public class UserPatchDTO {

    @Schema(description = "First name")
    private String firstName;

    @Schema(description = "Last name")
    private String lastName;

    @Schema(description = "Hourly rate")
    private long rate;

    @Schema(description = "Phone number")
    private String phone;

    @Schema(description = "Job title")
    private String jobTitle;

    @Schema(description = "User location", implementation = IdDTO.class)
    private Location location;

    @Schema(description = "User profile image", implementation = IdDTO.class)
    private File image;

    @Schema(description = "New password for the user")
    private String newPassword;
}
