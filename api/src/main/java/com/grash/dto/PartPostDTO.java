package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.Part;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating a new part")
public class PartPostDTO extends Part {

    @Schema(description = "Custom field values for the part")
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();
}