package com.grash.dto;

import com.grash.model.Meter;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReadingPatchDTO {

    private Double value;

    private Meter meter;
}
