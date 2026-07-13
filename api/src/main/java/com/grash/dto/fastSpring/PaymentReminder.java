package com.grash.dto.fastSpring;

import lombok.Data;

@Data
public class PaymentReminder {
    public String intervalUnit;
    public int intervalLength;
}
