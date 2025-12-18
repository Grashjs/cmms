package com.grash.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@SuperBuilder
@AllArgsConstructor
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
    @Builder.Default
    private List<String> assignedToEmails = new ArrayList<>();

    private String assetName;

    private String completedByEmail;
    private Double completedOn;
    private String archived;
    private String feedback;
    @Builder.Default
    private List<String> customersNames = new ArrayList<>();
}
