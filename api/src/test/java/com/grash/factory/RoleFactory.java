package com.grash.factory;

import com.grash.model.Role;
import com.grash.model.enums.PermissionEntity;
import com.grash.model.enums.RoleCode;
import com.grash.model.enums.RoleType;

import java.util.HashSet;
import java.util.Set;

public final class RoleFactory {

    private RoleFactory() {
    }

    public static Role createRole(String name, RoleType roleType) {
        return Role.builder()
                .name(name)
                .roleType(roleType)
                .code(RoleCode.USER_CREATED)
                .createPermissions(new HashSet<>())
                .viewPermissions(new HashSet<>())
                .viewOtherPermissions(new HashSet<>())
                .editOtherPermissions(new HashSet<>())
                .deleteOtherPermissions(new HashSet<>())
                .build();
    }

    public static Role createClientRole() {
        Role role = createRole("Test Client", RoleType.ROLE_CLIENT);
        role.getCreatePermissions().add(PermissionEntity.WORK_ORDERS);
        role.getViewPermissions().add(PermissionEntity.WORK_ORDERS);
        role.getViewPermissions().add(PermissionEntity.ASSETS);
        role.getViewPermissions().add(PermissionEntity.PARTS_AND_MULTIPARTS);
        role.getViewPermissions().add(PermissionEntity.LOCATIONS);
        role.getViewPermissions().add(PermissionEntity.SETTINGS);
        role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
        role.getEditOtherPermissions().add(PermissionEntity.WORK_ORDERS);
        role.getDeleteOtherPermissions().add(PermissionEntity.WORK_ORDERS);
        return role;
    }

    public static Role createReadOnlyRole() {
        Role role = createRole("Read Only", RoleType.ROLE_CLIENT);
        role.getViewPermissions().add(PermissionEntity.WORK_ORDERS);
        return role;
    }

    public static Role createAdminRole() {
        Role role = createRole("Admin", RoleType.ROLE_CLIENT);
        role.getCreatePermissions().add(PermissionEntity.WORK_ORDERS);
        role.getViewPermissions().addAll(Set.of(PermissionEntity.values()));
        role.getViewOtherPermissions().add(PermissionEntity.WORK_ORDERS);
        role.getEditOtherPermissions().add(PermissionEntity.WORK_ORDERS);
        role.getDeleteOtherPermissions().add(PermissionEntity.WORK_ORDERS);
        return role;
    }
}
