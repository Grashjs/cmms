package com.grash.dto;

import com.grash.model.enums.PermissionEntity;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Schema(description = "DTO for patching an existing role")
public class RolePatchDTO {

    @Schema(description = "Name")
    private String name;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "External identifier")
    private String externalId;

    @Schema(description = "List of entities with create permissions")
    private List<PermissionEntity> createPermissions;
    
    @Schema(description = "List of entities with view permissions")
    private List<PermissionEntity> viewPermissions;

    @Schema(description = "List of entities with view-other permissions")
    private List<PermissionEntity> viewOtherPermissions;

    @Schema(description = "List of entities with edit-other permissions")
    private List<PermissionEntity> editOtherPermissions;
    
    @Schema(description = "List of entities with delete-other permissions")
    private List<PermissionEntity> deleteOtherPermissions;
}
