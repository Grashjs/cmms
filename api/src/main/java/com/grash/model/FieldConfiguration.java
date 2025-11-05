package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.enums.FieldType;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"workOrderConfiguration", "workOrderRequestConfiguration"})

public class FieldConfiguration {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
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
