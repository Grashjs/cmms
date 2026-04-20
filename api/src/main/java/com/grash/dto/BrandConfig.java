package com.grash.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "Brand configuration for white-labeling")
public class BrandConfig {
    @Schema(description = "Brand name")
    public String name;
    
    @Schema(description = "Short brand name")
    public String shortName;
    
    @Schema(description = "Brand website")
    public String website;
    
    @Schema(description = "Brand email")
    public String mail;
    
    @Schema(description = "Brand address street")
    public String addressStreet;
    
    @Schema(description = "Brand phone number")
    public String phone;
    
    @Schema(description = "Brand address city")
    public String addressCity;
}
