package com.grash.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Super account relation linking parent and child users")
public class SuperAccountRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private User superUser;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER)
    private User childUser;
}


