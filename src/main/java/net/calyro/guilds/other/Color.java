package net.calyro.guilds.other;

import java.util.ArrayList;
import java.util.UUID;

import net.calyro.guilds.Guild;
import net.calyro.guilds.Level;

public enum Color {
	DARK_BLUE("1", null),
	DARK_RED("4", null),
	GOLD("6", null),
	DARK_PURPLE("5", null),
	LIGHT_PURPLE("d", null),
	WHITE("f", null),
	DARK_GRAY("8", null),
	RED("c", null),
	BLACK("0", null),
	GRAY("7", 0),
	BLUE("9", 15),
	DARK_GREEN("2", 25),
	YELLOW("e", 45),
	;

	private final String colorCode;
	private final Object requirement;

	Color(String colorCodE, Object rEquirement) {
		this.colorCode = colorCodE;
		this.requirement = rEquirement;
	}

	public final String getColorCode() {
		return colorCode;
	}

	public final Object getRequirement() {
		return requirement;
	}

	public static ArrayList<String> getUnlockedColors(UUID player, Guild guild) {
		ArrayList<String> unlocked = new ArrayList<>();
		int level = Level.getLevelFromXP(guild.getExperience());
		for (Color color : Color.values()) {
			if (color.getRequirement() == null) {
				unlocked.add(color.name()); // always unlocked
				continue;
			}
			if (level >= (int) color.getRequirement()) {
				unlocked.add(color.name());
			}
		}
		return unlocked;
	}


	public static ArrayList<String> getColoredUnlockedColors(UUID player, Guild guild) {
		ArrayList<String> unlocked = new ArrayList<>();
		int level = Level.getLevelFromXP(guild.getExperience());
		for (Color color : Color.values()) {
			if (color.getRequirement() == null) continue;
			if (level >= (int) color.getRequirement()) {
				unlocked.add("ยง" + color.getColorCode() + color.name());
			}
		}
		return unlocked;
	}

	public static ArrayList<String> getAllColoredColors() {
		ArrayList<String> unlocked = new ArrayList<>();
		for (Color color : Color.values()) {
			unlocked.add("ยง" + color.getColorCode() + color.name());
		}
		return unlocked;
	}

	public static String getColorCode(String name) {
		for (Color c : values()) {
			if (c.name().equalsIgnoreCase(name)) {
				return c.getColorCode();
			}
		}
		return null;
	}

	public static Color getTagColorByCode(String colorCode) {
		for (Color c : values()) {
			if (c.getColorCode().equals(colorCode)) {
				return c;
			}
		}
		return null;
	}
}
