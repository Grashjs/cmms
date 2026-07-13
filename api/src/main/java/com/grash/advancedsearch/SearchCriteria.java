package com.grash.advancedsearch;


import com.grash.model.OwnUser;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort.Direction;

import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchCriteria implements Cloneable {
    private List<FilterField> filterFields = new ArrayList<>();
    private Direction direction = Direction.ASC;
    private int pageNum = 0;
    private int pageSize = 10;
    private String sortField = "id";

    public void filterCompany(OwnUser user) {
        this.filterFields.add(FilterField.builder()
                .field("company")
                .value(user.getCompany().getId())
                .operation("eq")
                .values(new ArrayList<>()).build());
    }

    public void filterCreatedBy(OwnUser user) {
        this.filterFields.add(FilterField.builder()
                .field("createdBy")
                .value(user.getId())
                .operation("eq")
                .values(new ArrayList<>()).build());
    }

    @Override
    public SearchCriteria clone() {
        SearchCriteria result;
        try {
            result = (SearchCriteria) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException("Cloning failed", e);
        }
        result.filterFields = new ArrayList<>(this.filterFields);
        return result;
    }

}
