package com.zerosio.authentication.premium;

import java.util.UUID;

public class PremiumUser {

    private final UUID uuid;
    private final String name;
    private final boolean reliable;

    public PremiumUser(UUID uuid, String name, boolean reliable) {
        this.uuid = uuid;
        this.name = name;
        this.reliable = reliable;
    }

    public UUID getUniqueId() {
        return uuid;
    }

    public String getName() {
        return name;
    }

    public boolean isReliable() {
        return reliable;
    }

}
