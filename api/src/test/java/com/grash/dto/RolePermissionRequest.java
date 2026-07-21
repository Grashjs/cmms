package com.grash.dto;

import com.grash.model.enums.PermissionEntity;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RolePermissionRequest {
    @Builder.Default
    private List<PermissionEntity> create = List.of();
    @Builder.Default
    private List<PermissionEntity> view = List.of();
    @Builder.Default
    private List<PermissionEntity> viewOther = List.of();
    @Builder.Default
    private List<PermissionEntity> editOther = List.of();
    @Builder.Default
    private List<PermissionEntity> deleteOther = List.of();
}
