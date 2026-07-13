package com.grash.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class AuthResponse implements Serializable {

    private static final long serialVersionUID = 5926468583035150707L;
    private String accessToken;

    public AuthResponse(String accessToken){
        this.accessToken = accessToken;
    }
}
