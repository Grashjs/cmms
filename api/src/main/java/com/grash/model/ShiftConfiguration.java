package com.grash.model;

import com.grash.model.abstracts.Audit;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
@Table(name = "shift_configuration")
public class ShiftConfiguration extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @ElementCollection
    @CollectionTable(name = "shift_configuration_days",
            joinColumns = @JoinColumn(name = "shift_configuration_id"))
    private List<ShiftDayConfiguration> days = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "shift_configuration_exceptions",
            joinColumns = @JoinColumn(name = "shift_configuration_id"))
    private List<ShiftException> exceptions = new ArrayList<>();

    private boolean enabled = true;
}
