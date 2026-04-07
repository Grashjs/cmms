package com.grash.dto.fastSpring;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "FastSpring webhook event")
public class Event<T> {
    @Schema(description = "Event identifier")
    public String id;
    @Schema(description = "Whether the event has been processed")
    public boolean processed;
    @Schema(description = "Timestamp when the event was created")
    public long created;
    @Schema(description = "Event type identifier")
    public String type;
    @Schema(description = "Whether the event is from a live account (vs sandbox)")
    public boolean live;
    @Schema(description = "Event payload data")
    public T data;
}
