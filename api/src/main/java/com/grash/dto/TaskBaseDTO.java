package com.grash.dto;

import com.grash.model.Asset;
import com.grash.model.Meter;
import com.grash.model.OwnUser;
import com.grash.model.enums.TaskType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;

@Data
@NoArgsConstructor
public class TaskBaseDTO {
    @NotNull
    private String label;

    private TaskType taskType = TaskType.SUBTASK;

    private OwnUser user;

    private Asset asset;

    private Meter meter;

    private List<String> options;
}
