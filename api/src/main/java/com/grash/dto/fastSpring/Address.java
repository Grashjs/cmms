package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring address information")
public class Address {
    @Schema(description = "City name")
    public Object city;
    @Schema(description = "Country code")
    public String country;
    @Schema(description = "Postal/ZIP code")
    public Object postalCode;
    @Schema(description = "Region or state")
    public Object region;
    @Schema(description = "Company name")
    public Object company;
    @Schema(description = "Formatted address display")
    public String display;
}
