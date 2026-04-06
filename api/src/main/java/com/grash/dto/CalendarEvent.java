package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Generic calendar event wrapper")
public class CalendarEvent<T> {
    @Schema(description = "Event type")
    private String type;
    
    @Schema(description = "Event data")
    private T event;
    
    @Schema(description = "Event date")
    private Date date;
}
