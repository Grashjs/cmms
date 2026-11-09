package com.grash.dto.offline;

import com.grash.dto.CategoryMiniDTO;
import com.grash.dto.UserMiniDTO;
import com.grash.model.Labor;
import com.grash.model.enums.TimeStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.Date;
import java.util.UUID;

/** The prepared time list must never serialize lazy user/role/company graphs. */
@Getter
@AllArgsConstructor
public class MobileOfflineLabor {
    private final Long id;
    private final long version;
    private final UUID timerRunId;
    private final UserMiniDTO assignedTo;
    private final long hourlyRate;
    private final boolean includeToTotalTime;
    private final Date startedAt;
    private final CategoryMiniDTO timeCategory;
    private final boolean logged;
    private final TimeStatus status;
    private final long duration;
    private final Date createdAt;
    private final Date updatedAt;
    private final Long createdBy;
    private final Long updatedBy;

    public static MobileOfflineLabor from(Labor labor) {
        var user = labor.getAssignedTo();
        var category = labor.getTimeCategory();
        return new MobileOfflineLabor(labor.getId(), labor.getVersion(), labor.getTimerRunId(),
                user == null ? null : new UserMiniDTO(user.getId(), user.getFirstName(), user.getLastName(), null, null),
                labor.getHourlyRate(), labor.isIncludeToTotalTime(), labor.getStartedAt(),
                category == null ? null : new CategoryMiniDTO(category.getId(), category.getName()),
                labor.isLogged(), labor.getStatus(), labor.getDuration(), labor.getCreatedAt(), labor.getUpdatedAt(),
                labor.getCreatedBy(), labor.getUpdatedBy());
    }
}
