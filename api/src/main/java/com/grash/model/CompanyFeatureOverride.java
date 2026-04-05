package com.grash.model;

import com.grash.model.abstracts.Audit;
import com.grash.model.enums.PlanFeatures;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyFeatureOverride extends Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private PlanFeatures feature;

    @Column(nullable = false)
    private boolean enabled;
}
