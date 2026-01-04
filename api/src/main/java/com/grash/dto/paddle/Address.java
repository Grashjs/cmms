package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Address {
    private String description;

    @JsonProperty("first_line")
    private String firstLine;

    @JsonProperty("second_line")
    private String secondLine;

    private String city;

    @JsonProperty("postal_code")
    private String postalCode;

    private String region;

    @JsonProperty("country_code")
    private String countryCode;
}
