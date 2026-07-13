package com.grash.dto.imports;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
public class WorkOrderImportDTO {

    private Long id;
    @NotNull
    private String title;
    private String status;
    private String priority;
    private String description;
    private Double dueDate;
    private double estimatedDuration;
    private String requiredSignature;
    private String category;

    private String locationName;

    private String teamName;

    private String primaryUserEmail;

    private List<String> assignedToEmails;

    private String assetName;

    private String completedByEmail;
    private Double completedOn;
    private String archived;
    private String feedback;
    private List<String> customersNames;
}
