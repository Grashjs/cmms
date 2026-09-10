package com.grash.dto;

import com.grash.advancedsearch.FilterField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

@SuperBuilder
@Data
@NoArgsConstructor
@Schema(description = "Date range for filtering events and scheduled work orders")
public class DateRange {
    @Schema(description = "Start date of the range")
    private Date start;
    @Schema(description = "End date of the range")
    private Date end;
    @Schema(description = "Optional filter fields to apply additional filtering")
    private List<FilterField> filterFields;
}
