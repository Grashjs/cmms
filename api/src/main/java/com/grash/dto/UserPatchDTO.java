package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Location;
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

    private Location location;

    private File image;

    private String newPassword;
}
