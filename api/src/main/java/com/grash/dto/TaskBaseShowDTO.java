package com.grash.dto;

import com.grash.model.enums.TaskType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class TaskBaseShowDTO extends AuditShowDTO {
    private String label;

    private TaskType taskType;

    private List<TaskOptionShowDTO> options = new ArrayList<>();

    private UserMiniDTO user;

    private AssetMiniDTO asset;

    private MeterMiniDTO meter;
}
