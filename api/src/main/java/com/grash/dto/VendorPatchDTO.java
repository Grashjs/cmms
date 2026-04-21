package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.abstracts.BasicInfos;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Patch request data transfer object for updating vendor information")
public class VendorPatchDTO extends BasicInfos {

    @Schema(description = "Type of the vendor")
    private String vendorType;

    @Schema(description = "Description of the vendor")
    private String description;

    @Schema(description = "Rate charged by the vendor")
    private long rate;

    @Schema(description = "Custom field values for the vendor")
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();
}
