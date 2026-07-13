package com.grash.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class PushTokenPayload {
    @NotNull
    private String token;
}
