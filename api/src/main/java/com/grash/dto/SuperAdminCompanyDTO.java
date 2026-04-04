package com.grash.dto;

import lombok.Data;

@Data
public class SuperAdminCompanyDTO {
    private Long id;
    private String name;
    private String email;
    private int userCount;
}
