package com.grash.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuperAccountRelation {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER)
    private OwnUser superUser;

    @NotNull
    @OneToOne(fetch = FetchType.EAGER)
    private OwnUser childUser;
}
