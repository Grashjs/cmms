package com.grash.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.grash.model.abstracts.CompanyAudit;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Task option entity for task base choices")
public class TaskOption extends CompanyAudit {
    @Schema(description = "Option label")
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_base_id")
    @JsonBackReference
    private TaskBase taskBase;
}

