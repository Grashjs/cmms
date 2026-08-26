package com.grash.advancedsearch;

import com.grash.model.ApiKey;
import com.grash.model.Asset;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchFieldPolicyTest {

    @Test
    void validate_regularField_shouldNotThrow() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(User.class, "email"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(User.class, "firstName"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(User.class, "company.id"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "parentRequest.createdBy"));
    }

    @Test
    void validate_password_shouldBeRejected() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "password"));
    }

    @Test
    void validate_ApiKeyCode_shouldBeRejected() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(ApiKey.class, "code"));
    }

    @Test
    void validate_nestedPassword_shouldBeRejected() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "assignedTo.password"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "company.user.password"));
    }

    @Test
    void validate_otherSensitiveProperties_shouldBeRejected() {
        assertThrows(InvalidSearchFieldException.class, () -> SearchFieldPolicy.validate(User.class, "secret"));
        assertThrows(InvalidSearchFieldException.class, () -> SearchFieldPolicy.validate(WorkOrder.class, "token"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "company.secret"));
    }

    @Test
    void validate_disallowedUserField_shouldBeRejected() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "userSettings"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "appStats"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "superAccountRelations"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "parentSuperAccount"));
    }

    @Test
    void validate_pathNestedUnderDisallowedField_shouldBeRejected() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "userSettings.preferences"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(User.class, "superAccountRelations.childUser"));
    }

    @Test
    void validate_entityWithoutDisallowedEntries_shouldUseBlocklistOnly() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "title"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "anyField"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "assignedTo.password"));
    }

    @Test
    void validate_unknownEntity_shouldUseBlocklistOnly() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(Asset.class, "name"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(Asset.class, "anyProperty"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(Asset.class, "createdBy.password"));
    }

    @Test
    void validate_nullEntity_shouldUseBlocklistOnly() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(null, "name"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(null, "userSettings"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(null, "password"));
    }

    @Test
    void validate_nullOrBlankField_shouldBeRejected() {
        assertThrows(InvalidSearchFieldException.class, () -> SearchFieldPolicy.validate(User.class, null));
        assertThrows(InvalidSearchFieldException.class, () -> SearchFieldPolicy.validate(User.class, "  "));
    }

    @Test
    void validate_nestedUserRelation_shouldApplyUserBlocklist() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "primaryUser.userSettings"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "primaryUser.appStats"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "primaryUser.superAccountRelations"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(ApiKey.class, "user.userSettings"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(Asset.class, "primaryUser.parentSuperAccount"));
    }

    @Test
    void validate_nestedUserRelation_shouldAllowNonDisallowedPath() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "primaryUser.email"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(ApiKey.class, "user.firstName"));
    }

    @Test
    void validate_collectionRelation_shouldUnwrapElementType() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "assignedTo.userSettings"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "assignedTo.appStats"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(Asset.class, "assignedTo.superAccountRelations"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "team.users.parentSuperAccount"));
    }

    @Test
    void validate_collectionRelation_shouldAllowNonDisallowedPath() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "assignedTo.email"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "team.users.firstName"));
    }

    @Test
    void validate_deepNestedChain_shouldResolveEverySegment() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "asset.primaryUser.userSettings"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(WorkOrder.class, "team.users.userSettings"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "asset.primaryUser.email"));
    }

    @Test
    void validate_mapValueType_shouldUnwrapToValueClass() {
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(TestSearchEntity.class, "userMap.userSettings"));
        assertThrows(InvalidSearchFieldException.class,
                () -> SearchFieldPolicy.validate(TestSearchEntity.class, "userSet.appStats"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(TestSearchEntity.class, "userMap.email"));
    }

    @Test
    void validate_unresolvableSegment_shouldStopBlocklistTraversal() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(WorkOrder.class, "noSuchField.userSettings"));
    }

    @Test
    void validate_nonParameterizedOrNestedGenericCollection_shouldStopBlocklistTraversal() {
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(TestSearchEntity.class, "rawList.userSettings"));
        assertDoesNotThrow(() -> SearchFieldPolicy.validate(TestSearchEntity.class, "userListMap.appStats"));
    }

    @SuppressWarnings({"unused", "rawtypes"})
    private static class TestSearchEntity {
        private final java.util.Map<String, User> userMap = new java.util.HashMap<>();
        private final java.util.Set<User> userSet = new java.util.HashSet<>();
        private final java.util.List rawList = new java.util.ArrayList();
        private final java.util.Map<String, java.util.List<User>> userListMap = new java.util.HashMap<>();
    }
}
