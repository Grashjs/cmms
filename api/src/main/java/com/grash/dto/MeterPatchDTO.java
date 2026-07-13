package com.grash.dto;

import com.grash.model.File;
import com.grash.model.Location;
import com.grash.model.MeterCategory;
import com.grash.model.OwnUser;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collection;

@Data
@NoArgsConstructor
public class MeterPatchDTO {
    private String name;

    private String unit;

    private int updateFrequency;

    private MeterCategory meterCategory;

    private File image;

    private Location location;

    private Collection<OwnUser> users;

}
