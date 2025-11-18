package com.grash.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.CompanySettings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
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

    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private CompanySettings companySettings;

}
