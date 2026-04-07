package com.grash.dto.fastSpring;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring gift recipient details")
public class Recipient2 {
    @Schema(description = "Recipient first name")
    public String first;
    @Schema(description = "Recipient last name")
    public String last;
    @Schema(description = "Recipient email address")
    public String email;
    @Schema(description = "Recipient company name")
    public Object company;
    @Schema(description = "Recipient phone number")
    public String phone;
    @Schema(description = "Whether the recipient is subscribed to communications")
    public boolean subscribed;
    @Schema(description = "Recipient FastSpring account")
    public Account account;
    @Schema(description = "Recipient address details")
    public Address address;
}
