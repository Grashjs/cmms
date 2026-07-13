package com.grash.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.grash.model.abstracts.CompanyAudit;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
public class MultiParts extends CompanyAudit {

    @ManyToMany
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @JoinTable(name = "T_MultiParts_Part_Associations",
            joinColumns = @JoinColumn(name = "id_multi_parts"),
            inverseJoinColumns = @JoinColumn(name = "id_part"),
            indexes = {
                    @Index(name = "idx_multi_parts_part_multi_parts_id", columnList = "id_multi_parts"),
                    @Index(name = "idx_multi_parts_part_part_id", columnList = "id_part")
            })
    private List<Part> parts = new ArrayList<>();

    @NotNull
    private String name;

}
