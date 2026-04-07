package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.enums.FieldType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"workOrderConfiguration", "workOrderRequestConfiguration"})
@Schema(description = "Field configuration for customizing work order and request fields")
public class FieldConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Name of the field", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fieldName;

    @Builder.Default
    private FieldType fieldType = FieldType.OPTIONAL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private WorkOrderRequestConfiguration workOrderRequestConfiguration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private WorkOrderConfiguration workOrderConfiguration;


    public static Collection<FieldConfiguration> createFieldConfigurations(List<String> fieldNames, WorkOrderRequestConfiguration workOrderRequestConfiguration, WorkOrderConfiguration workOrderConfiguration) {
        return fieldNames.stream().map(fieldName -> FieldConfiguration
                .builder()
                .fieldName(fieldName)
                .workOrderRequestConfiguration(workOrderRequestConfiguration)
                .workOrderConfiguration(workOrderConfiguration)
                .build()).collect(Collectors.toList());
    }
}


