package com.grash.dto.offline;

import com.grash.dto.TaskShowDTO;
import com.grash.dto.WorkOrderHistoryShowDTO;
import com.grash.dto.comment.CommentShowDTO;
import com.grash.dto.workOrder.WorkOrderShowDTO;
import java.time.Instant;
import java.util.List;

public record MobileOfflineSnapshot(WorkOrderShowDTO workOrder, List<TaskShowDTO> tasks,
                                    List<CommentShowDTO> comments, List<WorkOrderHistoryShowDTO> history,
                                    Instant preparedAt, boolean complete, boolean canEdit) {}
