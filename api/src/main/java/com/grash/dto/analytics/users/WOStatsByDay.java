package com.grash.dto.analytics.users;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WOStatsByDay {
    private int created;
    private int completed;
    private Date date;
}
