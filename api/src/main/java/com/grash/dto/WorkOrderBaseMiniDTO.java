package com.grash.dto;

import com.grash.model.enums.Priority;
import com.grash.model.enums.Status;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Date;

@Data
@NoArgsConstructor
public class WorkOrderBaseMiniDTO {
    private Long id;
    private String title;
    private Date dueDate;
    private Instant createdAt;
    private Priority priority;
}
