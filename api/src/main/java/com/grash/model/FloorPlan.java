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
@Schema(description = "Floor plan entity for storing location layout information")
public class FloorPlan {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Floor plan name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @OneToOne(fetch = FetchType.LAZY)
    private File image;

    @Schema(description = "Total area of the floor plan")
    private long area;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Location location;
}


