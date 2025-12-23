package com.grash.dto.license;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RelationshipWithMeta {
    private Map<String, String> links;
    private Map<String, Integer> meta;
}
