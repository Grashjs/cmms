package com.grash.model;

import com.grash.model.abstracts.Audit;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Data
@NoArgsConstructor
@Builder
@AllArgsConstructor
public class UserInvitation extends Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private String email;

    @ManyToOne(fetch = FetchType.LAZY)
    @NotNull
    private Role role;

    public UserInvitation(String email, Role role) {
        this.email = email;
        this.role = role;
    }
}
