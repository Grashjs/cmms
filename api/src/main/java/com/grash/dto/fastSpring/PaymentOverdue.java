package com.grash.dto.fastSpring;

import lombok.Data;

@Data
public class PaymentOverdue {
    public String intervalUnit;
    public int intervalLength;
    public int total;
    public int sent;
}
