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
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CommentPatchDTO {

    @NotBlank
    private String content;

    private List<File> files = new ArrayList<>();

}
