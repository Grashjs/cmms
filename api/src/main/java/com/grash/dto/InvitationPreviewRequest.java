package com.grash.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
public class InvitationPreviewRequest {
    @NotBlank
    @ToString.Exclude
    private String token;
}
