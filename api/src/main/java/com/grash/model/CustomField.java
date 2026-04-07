package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Custom field entity for storing vendor-specific key-value pairs")
public class CustomField {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @Schema(description = "Field name", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String name;

    @Schema(description = "Field value", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vendor vendor;

}


