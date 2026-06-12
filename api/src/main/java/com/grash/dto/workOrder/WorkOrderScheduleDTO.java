package com.grash.dto.workOrder;

import com.grash.dto.UserMiniDTO;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class WorkOrderScheduleDTO {

    private Long userId;
    private String userFirstName;
    private String userLastName;
    private Date estimatedStartDate;
}
