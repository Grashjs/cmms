package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "Authentication response containing access token")
public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 5926468583035150707L;
    
    @Schema(description = "JWT access token for authenticated requests")
    private String accessToken;

    public AuthResponse(String accessToken){
        this.accessToken = accessToken;
    }
}
