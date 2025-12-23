package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EntitlementsResponse {
    private List<EntitlementData> data;
}
