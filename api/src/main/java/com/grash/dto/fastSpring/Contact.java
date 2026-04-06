package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring contact information")
public class Contact {
    @Schema(description = "First name")
    public String first;
    @Schema(description = "Last name")
    public String last;
    @Schema(description = "Email address")
    public String email;
    @Schema(description = "Company name")
    public Object company;
    @Schema(description = "Phone number")
    public String phone;
    @Schema(description = "Whether the contact is subscribed to communications")
    public boolean subscribed;
}
