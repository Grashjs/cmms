package com.grash.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.Arrays;

public enum Language {
    EN,
    FR,
    TR,
    ES,
    PT_BR,
    PL,
    DE,
    AR,
    IT,
    SV,
    RU,
    PT,
    HU,
    NL,
    ZH_CN,
    ZH;
    //always add new languages at the end
    
    @JsonCreator
    public static Language fromString(String value) {
        return Arrays.stream(Language.values())
                .filter(lang -> lang.name().equalsIgnoreCase(value))
                .findFirst()
                .orElse(Language.EN); // This is your fallback
    }
}
