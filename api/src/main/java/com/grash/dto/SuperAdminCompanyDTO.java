package com.grash.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SuperAdminCompanyDTO {
    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
    private String subscriptionPlanName;
    private int userCount;
}
