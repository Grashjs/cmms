package com.grash.dto.comment;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCount {
    private long count;
    private long withFilesCount;
}
