package com.grash.dto;

import lombok.Data;

@Data
public class UtmParams {
    private String utm_source;
    private String utm_medium;
    private String utm_campaign;
    private String utm_term;
    private String utm_content;
    private String gclid;
    private String fbclid;
}
