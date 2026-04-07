package com.grash.dto.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
@Schema(description = "DTO for importing work orders from external data sources")
public class WorkOrderImportDTO {

    @Schema(description = "Unique identifier")
    private Long id;
    
    @Schema(description = "Title")
    @NotNull
    private String title;
    
    @Schema(description = "Status")
    private String status;
    
    @Schema(description = "Priority")
    private String priority;
    
    @Schema(description = "Description")
    private String description;
    
    @Schema(description = "Due date (timestamp)")
    private Double dueDate;
    
    @Schema(description = "Estimated duration")
    private double estimatedDuration;
    
    @Schema(description = "Required signature")
    private String requiredSignature;
    
    @Schema(description = "Category name")
    private String category;

    @Schema(description = "Location name")
    private String locationName;

    @Schema(description = "Team name")
    private String teamName;

    @Schema(description = "Primary user email")
    private String primaryUserEmail;
    
    @Schema(description = "List of assigned user emails")
    @Builder.Default
    private List<String> assignedToEmails = new ArrayList<>();

    @Schema(description = "Asset name")
    private String assetName;

    @Schema(description = "Completed by email")
    private String completedByEmail;
    
    @Schema(description = "Completed on (timestamp)")
    private Double completedOn;
    
    @Schema(description = "Whether the work order is archived")
    private String archived;
    
    @Schema(description = "Feedback")
    private String feedback;
    
    @Schema(description = "List of customer names")
    @Builder.Default
    private List<String> customersNames = new ArrayList<>();
}

