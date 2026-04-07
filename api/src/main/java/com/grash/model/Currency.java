package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Schema(description = "Currency entity for financial transactions")
public class Currency {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Column(unique = true)
    @Schema(description = "Currency name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Column(unique = true)
    @Schema(description = "Currency code (e.g., USD, EUR)")
    private String code;
}


