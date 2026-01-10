package com.grash.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.grash.model.abstracts.CompanyAudit;
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
public class TaskOption extends CompanyAudit {
    private String label;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_base_id")
    @JsonBackReference
    private TaskBase taskBase;
}

