package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring gift recipient wrapper")
public class Recipient {
    @Schema(description = "Recipient details")
    public Recipient recipient;
}
