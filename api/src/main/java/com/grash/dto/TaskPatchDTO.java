package com.grash.dto;

import com.grash.model.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing task")
public class TaskPatchDTO {

    @Schema(description = "Task status")
    private Status status;

    @Schema(description = "Task notes")
    private String notes;

    @Schema(description = "Task value")
    @Size(max = 255)
    private String value;

    @Min(0)
    private Long version;

    @JsonIgnore
    private boolean notesPresent;
    @JsonIgnore
    private boolean valuePresent;

    public void setNotes(String notes) {
        this.notes = notes;
        this.notesPresent = true;
    }

    public void setValue(String value) {
        this.value = value;
        this.valuePresent = true;
    }

    @JsonIgnore
    public boolean hasNotes() { return notesPresent; }

    @JsonIgnore
    public boolean hasValue() { return valuePresent; }

}
