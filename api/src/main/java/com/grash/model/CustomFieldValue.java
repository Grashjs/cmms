package com.grash.model;

import com.grash.model.abstracts.Audit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Custom field value for entities")
public class CustomFieldValue extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @Schema(description = "Custom field definition", requiredMode = Schema.RequiredMode.REQUIRED)
    private CustomField customField;

    @Schema(description = "Value of the custom field")
    @Column(columnDefinition = "TEXT")
    private String value;

    @Schema(description = "Work order this value belongs to")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private WorkOrder workOrder;

    @Schema(description = "Asset this value belongs to")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Asset asset;

    @Schema(description = "Location this value belongs to")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Location location;

    @Schema(description = "Customer this value belongs to")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Customer customer;

    @Schema(description = "Vendor this value belongs to")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Vendor vendor;

    @Schema(description = "Part this value belongs to")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Part part;

    @Schema(description = "Meter this value belongs to")
    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Meter meter;
}