package com.grash.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkOrderMessagePatchDTO {

    private String content;

    private boolean deleted;
}
