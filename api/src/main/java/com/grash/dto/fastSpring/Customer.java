package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring customer information")
public class Customer {
    @Schema(description = "Customer first name")
    public String first;
    @Schema(description = "Customer last name")
    public String last;
    @Schema(description = "Customer email address")
    public String email;
    @Schema(description = "Customer company name")
    public Object company;
    @Schema(description = "Customer phone number")
    public String phone;
    @Schema(description = "Whether the customer is subscribed to communications")
    public boolean subscribed;
}
