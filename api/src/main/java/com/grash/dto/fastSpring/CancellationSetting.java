package com.grash.dto.fastSpring;

import lombok.Data;

@Data
public class CancellationSetting {
    public String cancellation;
    public String intervalUnit;
    public int intervalLength;
}
