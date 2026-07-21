package com.grash.factory;

import com.grash.model.Part;

public final class PartFactory {

    private PartFactory() {
    }

    public static Part createPart(String name) {
        Part part = new Part();
        part.setName(name);
        return part;
    }
}
