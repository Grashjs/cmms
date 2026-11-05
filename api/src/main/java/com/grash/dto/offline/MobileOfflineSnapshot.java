package com.grash.dto.offline;

import com.grash.dto.TaskShowDTO;
import com.grash.dto.WorkOrderHistoryShowDTO;
import com.grash.dto.comment.CommentShowDTO;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import java.time.Instant;
import java.util.List;

public record MobileOfflineSnapshot(WorkOrderShowDTO workOrder, List<TaskShowDTO> tasks,
                                    List<CommentShowDTO> comments, List<WorkOrderHistoryShowDTO> history,
                                    Instant preparedAt, boolean complete, boolean canEdit, boolean canComment,
                                    List<MobileOfflineAsset> assets, boolean canScan, boolean canReadAssets,
                                    List<MobileOfflineLabor> labors, MobileOfflineTimePermissions timePermissions,
                                    long timerStatusRevision, MobileOfflineClosure closure) {
    public MobileOfflineSnapshot(WorkOrderShowDTO workOrder, List<TaskShowDTO> tasks, List<CommentShowDTO> comments,
                                 List<WorkOrderHistoryShowDTO> history, Instant preparedAt, boolean complete,
                                 boolean canEdit, boolean canComment, List<MobileOfflineAsset> assets, boolean canScan,
                                 boolean canReadAssets, List<MobileOfflineLabor> labors,
                                 MobileOfflineTimePermissions timePermissions, long timerStatusRevision) {
        this(workOrder, tasks, comments, history, preparedAt, complete, canEdit, canComment, assets, canScan,
                canReadAssets, labors, timePermissions, timerStatusRevision, null);
    }
    public MobileOfflineSnapshot(WorkOrderShowDTO workOrder, List<TaskShowDTO> tasks, List<CommentShowDTO> comments,
                                 List<WorkOrderHistoryShowDTO> history, Instant preparedAt, boolean complete,
                                 boolean canEdit, boolean canComment, List<MobileOfflineAsset> assets, boolean canScan,
                                 boolean canReadAssets) {
        this(workOrder, tasks, comments, history, preparedAt, complete, canEdit, canComment, assets, canScan,
                canReadAssets, List.of(), new MobileOfflineTimePermissions(false, false, false, false), 0);
    }
    public MobileOfflineSnapshot(WorkOrderShowDTO workOrder, List<TaskShowDTO> tasks, List<CommentShowDTO> comments,
                                 List<WorkOrderHistoryShowDTO> history, Instant preparedAt, boolean complete,
                                 boolean canEdit, boolean canComment, List<MobileOfflineAsset> assets, boolean canScan) {
        this(workOrder, tasks, comments, history, preparedAt, complete, canEdit, canComment, assets, canScan, false);
    }

    public MobileOfflineSnapshot(WorkOrderShowDTO workOrder, List<TaskShowDTO> tasks, List<CommentShowDTO> comments,
                                 List<WorkOrderHistoryShowDTO> history, Instant preparedAt, boolean complete,
                                 boolean canEdit, boolean canComment) {
        this(workOrder, tasks, comments, history, preparedAt, complete, canEdit, canComment, List.of(), false, false);
    }
}
