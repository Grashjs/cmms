package com.grash.model;

import com.grash.model.abstracts.CategoryAbstract;
import com.grash.model.abstracts.CompanyAudit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class SubscriptionChangeRequest extends CompanyAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String code;

    @NotNull
    private Boolean monthly;

    @NotNull
    private int usersCount;
}
