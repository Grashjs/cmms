package com.grash.dto.comment;

import com.grash.dto.FileShowDTO;
import com.grash.dto.UserMiniDTO;
import com.grash.model.File;
import com.grash.model.User;
import com.grash.model.WorkOrder;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import com.grash.dto.AuditShowDTO;

import java.util.ArrayList;
import java.util.List;

@Data
public class CommentShowDTO extends AuditShowDTO {

    private UserMiniDTO user;

    private String content;

    private List<FileShowDTO> files = new ArrayList<>();

    private boolean system;

}
