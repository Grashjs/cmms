package com.grash.dto;

import com.grash.dto.cutomField.CustomFieldValueShowDTO;
import com.grash.model.WorkOrderCategory;
import com.grash.model.enums.Priority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "Base work order display data transfer object")
public class WorkOrderBaseShowDTO extends AuditShowDTO {
    @Schema(description = "Due date of the work order")
    private Date dueDate;
    @Schema(description = "Priority level of the work order")
    private Priority priority = Priority.NONE;
    @Schema(description = "Estimated duration in hours")
    private double estimatedDuration;
    @Schema(description = "Estimated start date for the work order")
    private Date estimatedStartDate;
    @Schema(description = "Detailed description of the work order")
    private String description;
    @Schema(description = "Title of the work order")
    private String title;
    @Schema(description = "Indicates if a signature is required")
    private boolean requiredSignature;

    @Schema(description = "Category associated with the work order")
    private CategoryMiniDTO category;

    @Schema(description = "Location where the work will be performed")
    private LocationMiniDTO location;

    @Schema(description = "Team assigned to the work order")
    private TeamMiniDTO team;

    @Schema(description = "Primary user responsible for the work order")
    private UserMiniDTO primaryUser;

    @Schema(description = "List of users assigned to the work order")
    private List<UserMiniDTO> assignedTo;

    @Schema(description = "List of customers associated with the work order")
    private List<CustomerMiniDTO> customers;

    @Schema(description = "Asset related to the work order")
    private AssetMiniDTO asset;

    @Schema(description = "List of files attached to the work order")
    private List<FileMiniDTO> files;

    @Schema(description = "Image associated with the work order")
    private FileMiniDTO image;
    
    private List<CustomFieldValueShowDTO> customFieldValues = new ArrayList<>();
}
