package com.grash.dto.comment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCriteria {
    @NotNull
    private Long workOrderId;
}
