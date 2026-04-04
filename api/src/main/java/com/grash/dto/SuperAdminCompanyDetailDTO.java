package com.grash.dto;

import lombok.Data;
import java.util.List;

@Data
public class SuperAdminCompanyDetailDTO extends SuperAdminCompanyDTO {
    private List<UserResponseDTO> users;
}
