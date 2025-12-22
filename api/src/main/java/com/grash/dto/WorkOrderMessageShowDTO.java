package com.grash.dto;

import com.grash.model.enums.MessageType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class WorkOrderMessageShowDTO {

    private Long id;

    private Long workOrderId;

    private UserMiniDTO user;

    private MessageType messageType;

    private String content;

    private FileMiniDTO file;

    private Long parentMessageId;

    private boolean edited;

    private boolean deleted;

    private Date createdAt;

    private Date updatedAt;

    private List<WorkOrderMessageReactionDTO> reactions;

    private List<UserMiniDTO> readBy;

    private boolean readByCurrentUser;
}
