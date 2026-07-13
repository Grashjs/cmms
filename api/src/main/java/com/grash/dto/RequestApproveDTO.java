package com.grash.dto;

import com.grash.model.enums.AssetStatus;
import lombok.Data;

@Data
public class RequestApproveDTO {
    private AssetStatus assetStatus;
}
