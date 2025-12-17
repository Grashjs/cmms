package com.grash.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class PreventiveMaintenanceImportDTO {

    private Long id;

    @NotNull
    private String name;

    @NotNull
    private String title;

    private String description;

    private String priority;

    private Double estimatedDuration;

    private String requiredSignature;

    private String category;

    private String locationName;

    private String teamName;

    private String primaryUserEmail;

    @Builder.Default
    private List<String> assignedToEmails = new ArrayList<>();

    private String assetName;

    private Double expectedStartDate; // Expected start date for generated WOs

    private String checklistName; // Name of checklist to copy tasks from

    @NotNull
    private Double startsOn;

    @NotNull
    private Integer frequency;

    @NotNull
    private String recurrenceType;

    private String recurrenceBasedOn;

    private Integer dueDateDelay;

    private Double endsOn;

    @Builder.Default
    private List<Integer> daysOfWeek = new ArrayList<>();
}
