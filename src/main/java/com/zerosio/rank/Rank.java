package com.zerosio.rank;

public enum Rank {

    DEFAULT("§7", "§7"),
    VIP("§a", "VIP"),
    VIP_PLUS("§a", "VIP§6+"),
    MVP("§b", "MVP"),
    MVP_PLUS("§b", "MVP§c+"),
    MVP_PLUS_PLUS("§6", "MVP§c++"),
    YOUTUBE("§c", "§fYOUTUBE"),
    HELPER("§9", "HELPER"),
    MOD("§2", "MOD"),
    GAMEMASTER("§2", "GM"),
    BETATESTER("§d", "BT"),
    ADMIN("§c", "ADMIN"),
    DEPUTY("§c", "DEPUTY"),
    COOWNER("§c", "COOWNER"),
    JERRY("§d", "JERRY§c++"),
    OWNER("§c", "OWNER");

    private final String prefix;
    private final String color;

    Rank(String color, String prefix) {
        this.color = color;
        this.prefix = prefix;
    }

    public static Rank getRankOrDefault(int level) {
        for (Rank rank : Rank.values()) {
            if (rank.getLevel() == level) {
                return rank;
            }
        }
        return DEFAULT;
    }

    public int getLevel() {
        return this.ordinal() + 1;
    }

    public String getScoreRank() {
        return this == DEFAULT ? "§7Default" : getPrefixColoured();
    }

    public String getPrefix() {
        return this == DEFAULT ? color : color + "[" + prefix + color + "] ";
    }

    public String getColour() {
        return color;
    }

    public String getPrefixx() {
        return prefix;
    }

    public String getPrefixColoured() {
        return color + prefix;
    }

    public boolean isBelowOrEqual(Rank rank) {
        return this.getLevel() <= rank.getLevel();
    }

    public boolean isAboveOrEqual(Rank rank) {
        return this.getLevel() >= rank.getLevel();
    }

    public boolean hasRank(Rank requiredRank) {
        return this.getLevel() >= requiredRank.getLevel();
    }

    public boolean isStaff() {
        return this.ordinal() >= HELPER.ordinal();
    }

    public boolean isDefaultPermission() {
        return this == DEFAULT;
    }

    public String getFormattedRank() {
        return prefix;
    }
}
