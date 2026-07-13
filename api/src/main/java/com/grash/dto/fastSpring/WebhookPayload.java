package com.grash.dto.fastSpring;

import lombok.Data;

import java.util.ArrayList;

@Data
public class WebhookPayload<T> {
    public ArrayList<Event<T>> events;
}
