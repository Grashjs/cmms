package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring tags associated with an order or subscription")
public class Tags {
    @Schema(description = "User identifier stored as a tag")
    public int userId;
}
