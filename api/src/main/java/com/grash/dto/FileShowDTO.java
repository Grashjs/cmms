package com.grash.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.grash.model.*;
import com.grash.model.enums.FileType;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Data
public class FileShowDTO extends AuditShowDTO {
    private String name;

    private String url;

    private FileType type = FileType.OTHER;

    private boolean hidden = false;

}
