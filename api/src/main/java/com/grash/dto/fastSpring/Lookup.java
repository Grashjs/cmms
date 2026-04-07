package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;

@lombok.Data
@Schema(description = "FastSpring lookup reference data")
public class Lookup {
    @Schema(description = "Global lookup identifier")
    public String global;
}
