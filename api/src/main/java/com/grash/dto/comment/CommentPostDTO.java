package com.grash.dto.comment;

import com.grash.model.File;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommentPostDTO {
    @NotNull
    private WorkOrder workOrder;

    @NotBlank
    @Column(columnDefinition = "TEXT")
    private String content;

    @OneToMany
    private List<File> files = new ArrayList<>();

}
