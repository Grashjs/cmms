package com.grash.dto.fastSpring;

import lombok.Data;

@Data
public class Event<T> {
    public String id;
    public boolean processed;
    public long created;
    public String type;
    public boolean live;
    public T data;
}
