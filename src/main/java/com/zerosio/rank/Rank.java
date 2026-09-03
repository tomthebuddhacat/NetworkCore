package com.zerosio.rank;

import com.zerosio.Config;

public enum Rank {

    DEFAULT("default"),
    VIP("vip"),
    VIP_PLUS("vip_plus"),
    MVP("mvp"),
    MVP_PLUS("mvp_plus"),
    MVP_PLUS_PLUS("mvp_double_plus"),
    YOUTUBE("media_youtube"),
    HELPER("helper"),
    MOD("mod"),
    GAMEMASTER("gm"),
    BETATESTER("bt"),
    ADMIN("admin"),
    OWNER("owner");

    private final String rankName;

    Rank(String rankName) {
        this.rankName = rankName;
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
        return this == DEFAULT ? "<gray>" : getPrefixColored();
    }

    public String getPrefix() {
        return Config.getString("ranks." + rankName + ".rankDisplay", "");
    }

    public String getPrefixx() {
        return getPrefix();
    }

    public String getPrefixColored() {
        return getPrefix();
    }

    public String getColour() {
        return getPrefixColored();
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
        return getPrefix();
    }
}
