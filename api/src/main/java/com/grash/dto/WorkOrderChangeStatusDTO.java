package com.grash.dto;

import com.grash.model.File;
import com.grash.model.OwnUser;
import com.grash.model.enums.Status;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class WorkOrderChangeStatusDTO {
    private Status status;
    private String signature;
    private String feedback;
}