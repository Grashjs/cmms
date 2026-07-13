package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import lombok.*;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "companySettings")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotNull
    private RoleType roleType;

    private RoleCode code = RoleCode.USER_CREATED;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private boolean paid;

    @NotNull
    private String name;

    private String description;

    private String externalId;

    @ElementCollection(targetClass = PermissionEntity.class)
    private Set<PermissionEntity> createPermissions = new HashSet<>();

    @ElementCollection(targetClass = PermissionEntity.class)
    private Set<PermissionEntity> viewPermissions = new HashSet<>();

    @ElementCollection(targetClass = PermissionEntity.class)
    private Set<PermissionEntity> viewOtherPermissions = new HashSet<>();

    @ElementCollection(targetClass = PermissionEntity.class)
    private Set<PermissionEntity> editOtherPermissions = new HashSet<>();

    @ElementCollection(targetClass = PermissionEntity.class)
    private Set<PermissionEntity> deleteOtherPermissions = new HashSet<>();


    @ManyToOne
    @NotNull
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private CompanySettings companySettings;
}
