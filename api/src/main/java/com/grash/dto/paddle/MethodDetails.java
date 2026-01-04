package com.grash.dto.paddle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MethodDetails {
    private String type;
    private Card card;

    @JsonProperty("south_korea_local_card")
    private Object southKoreaLocalCard;
}
