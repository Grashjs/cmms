package com.grash.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.CompanySettings;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class ChecklistPostDTO {
    private String name;

    private String description;

    private List<TaskBaseDTO> taskBases;

    private String category;

    @Schema(implementation = IdDTO.class)
    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private CompanySettings companySettings;

}

