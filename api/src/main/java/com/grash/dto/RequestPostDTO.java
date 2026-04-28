package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.Request;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RequestPostDTO extends Request {

    @Schema(description = "List of custom field values")
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();
}