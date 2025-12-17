package com.grash.dto;

import com.grash.model.enums.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
public class WorkOrderMessagePostDTO {

    @NotNull
    private Long workOrderId;

    @NotNull
    private MessageType messageType;

    private String content;

    private Long fileId;

    private Long parentMessageId;
}
