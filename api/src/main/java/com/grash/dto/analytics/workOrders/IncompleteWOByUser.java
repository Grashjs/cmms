package com.grash.dto.analytics.workOrders;

import com.grash.dto.UserMiniDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncompleteWOByUser extends UserMiniDTO {
    private int count;
    private long averageAge;
}
