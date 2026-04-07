package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Paddle address information")
public class Address {
    @Schema(description = "Description of the address")
    private String description;

    @Schema(description = "First line of the address")
    @JsonProperty("first_line")
    private String firstLine;

    @Schema(description = "Second line of the address")
    @JsonProperty("second_line")
    private String secondLine;

    @Schema(description = "City name")
    private String city;

    @Schema(description = "Postal or ZIP code")
    @JsonProperty("postal_code")
    private String postalCode;

    @Schema(description = "State or region")
    private String region;

    @Schema(description = "ISO country code")
    @JsonProperty("country_code")
    private String countryCode;
}
