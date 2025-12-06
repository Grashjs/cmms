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
    private String referrer;

    public boolean hasAnyParam() {
        return isNotEmpty(utm_source) ||
                isNotEmpty(utm_medium) ||
                isNotEmpty(utm_campaign) ||
                isNotEmpty(utm_term) ||
                isNotEmpty(utm_content) ||
                isNotEmpty(gclid) ||
                isNotEmpty(fbclid) ||
                isNotEmpty(referrer);
    }

    private boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

}
