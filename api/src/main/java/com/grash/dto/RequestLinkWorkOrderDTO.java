package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO for linking a maintenance request to an already existing work order")
public class RequestLinkWorkOrderDTO {
    @NotNull
    @Schema(description = "ID of the existing work order (open or closed) the request should be linked to")
    private Long workOrderId;
}
