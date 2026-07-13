package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class WorkOrderMiniDTO {
    private Long id;
    private String title;
    private Date dueDate;
    private String customId;

}
