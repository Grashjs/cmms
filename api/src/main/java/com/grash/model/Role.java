package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "companySettings")
@Schema(description = "Role entity defining user permissions and access levels")
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Schema(description = "Unique identifier", accessMode = Schema.AccessMode.READ_ONLY)
    private Long id;

    @NotNull
    @Schema(description = "Type of role", requiredMode = Schema.RequiredMode.REQUIRED)
    private RoleType roleType;

    @Schema(description = "Role code identifier")
    private RoleCode code = RoleCode.USER_CREATED;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Schema(description = "Whether this is a paid role", accessMode = Schema.AccessMode.READ_ONLY)
    private boolean paid;

    @NotNull
    @Schema(description = "Role name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Role description")
    private String description;

    @Schema(description = "External ID for integration purposes")
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


    @ManyToOne(fetch = FetchType.LAZY)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private CompanySettings companySettings;

    public boolean belongsToCompany(Company company) {
        return this.companySettings == null ||
                belongsOnlyToCompany(company);
    }

    public boolean belongsOnlyToCompany(Company company) {
        return this.companySettings != null
                && company.getCompanySettings().getId().equals(this.companySettings.getId());
    }
}


