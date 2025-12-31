package com.zerosio.mojang.data;

import java.util.UUID;

public class PremiumData {
    private final boolean premium;
    private final UUID uuid;

    public PremiumData(boolean premium, UUID uuid) {
        this.premium = premium;
        this.uuid = uuid;
    }

    public boolean isPremium() { return premium; }
    public UUID getUuid() { return uuid; }
}
