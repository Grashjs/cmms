package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValuePostDTO;
import com.grash.model.Customer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for creating a new customer")
public class CustomerPostDTO extends Customer {

    @Schema(description = "Custom field values for the customer")
    private List<CustomFieldValuePostDTO> customFields = new ArrayList<>();
}