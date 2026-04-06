package com.grash.dto;

import com.grash.model.enums.AssetStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "DTO for approving a maintenance request")
public class RequestApproveDTO {
    @Schema(description = "Status to set on the asset associated with the request")
    private AssetStatus assetStatus;
}
