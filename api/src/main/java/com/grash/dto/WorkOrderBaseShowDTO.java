package com.grash.dto;

import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.Priority;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class WorkOrderBaseShowDTO extends AuditShowDTO {
    private Date dueDate;
    private Priority priority = Priority.NONE;
    private double estimatedDuration;
    private Date estimatedStartDate;
    private String description;
    private String title;
    private boolean requiredSignature;

    private WorkOrderCategory category;

    private LocationMiniDTO location;

    private TeamMiniDTO team;

    private UserMiniDTO primaryUser;

    private List<UserMiniDTO> assignedTo;

    private List<CustomerMiniDTO> customers;

    private AssetMiniDTO asset;

    private List<FileMiniDTO> files;

    private FileMiniDTO image;
}
