package com.grash.dto;

import lombok.Data;
import java.util.List;

@Data
public class SuperAdminCompanyDetailDTO {
    private Long id;
    private String name;
    private String email;
    private List<SuperAdminUserDTO> users;

    @Data
    public static class SuperAdminUserDTO {
        private Long id;
        private String email;
        private String firstName;
        private String lastName;
        private String role;
    }
}
