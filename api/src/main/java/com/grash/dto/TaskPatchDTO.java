package com.grash.dto;

import com.grash.model.enums.Status;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TaskPatchDTO {

    private Status status;

    private String notes;

    private String value;

}
