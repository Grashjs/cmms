package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.Meter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating a new meter")
public class MeterPostDTO extends Meter {

    @Schema(description = "Custom field values for the meter")
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();
}