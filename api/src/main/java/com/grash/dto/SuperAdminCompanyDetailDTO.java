package com.grash.dto;

import lombok.Data;
import java.util.Date;
import java.util.List;

@Data
public class SuperAdminCompanyDetailDTO {
    private Long id;
    private String name;
    private String email;
    private Long subscriptionPlanId;
    private String subscriptionPlanName;
    private int usersLimit;
    private int userCount;
    private Date expiryDate;
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
