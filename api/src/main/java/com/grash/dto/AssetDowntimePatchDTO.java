package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class AssetDowntimePatchDTO {
    private long duration;

    private Date startsOn;
}
