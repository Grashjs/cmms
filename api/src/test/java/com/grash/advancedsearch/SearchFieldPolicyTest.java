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
}
