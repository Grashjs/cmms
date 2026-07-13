package com.grash.advancedsearch;

import com.grash.model.enums.EnumName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.criteria.JoinType;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class FilterField {
    private String field;
    private JoinType joinType;
    private Object value;
    private String operation;
    private List<Object> values = new ArrayList<>();
    private List<FilterField> alternatives;
    private EnumName enumName;
}
