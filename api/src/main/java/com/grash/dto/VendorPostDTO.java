package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.Vendor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating a new vendor")
public class VendorPostDTO extends Vendor {

    @Schema(description = "Custom field values for the vendor")
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();
}