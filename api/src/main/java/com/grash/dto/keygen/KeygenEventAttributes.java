package com.grash.dto.keygen;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeygenEventAttributes {
    private String event;
    private String payload;
}
