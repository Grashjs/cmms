package com.grash.dto.fastSpring;


import lombok.Data;

@Data
public class Recipient2 {
    public String first;
    public String last;
    public String email;
    public Object company;
    public String phone;
    public boolean subscribed;
    public Account account;
    public Address address;
}
