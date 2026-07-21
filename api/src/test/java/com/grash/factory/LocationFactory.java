package com.grash.factory;

import com.grash.model.Location;

public final class LocationFactory {

    private LocationFactory() {
    }

    public static Location createLocation(String name) {
        Location location = new Location();
        location.setName(name);
        return location;
    }
}
