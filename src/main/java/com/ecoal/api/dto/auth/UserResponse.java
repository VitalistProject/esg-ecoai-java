package com.ecoal.api.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private String role;

    @JsonProperty("company_id")
    private Long companyId;

    private String department;
}
