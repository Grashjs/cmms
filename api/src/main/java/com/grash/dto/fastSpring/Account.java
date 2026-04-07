package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring payment account information")
public class Account {
    @Schema(description = "Account identifier")
    public String id;
    @Schema(description = "Account reference")
    public String account;
    @Schema(description = "Contact information for the account")
    public Contact contact;
    @Schema(description = "Account address details")
    public Address address;
    @Schema(description = "Account language preference")
    public String language;
    @Schema(description = "Account country code")
    public String country;
    @Schema(description = "Lookup information for the account")
    public Lookup lookup;
    @Schema(description = "Account URL")
    public String url;
}
