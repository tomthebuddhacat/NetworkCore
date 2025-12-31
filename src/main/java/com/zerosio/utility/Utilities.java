package com.zerosio.utility;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import com.zerosio.rank.Rank;
import com.zerosio.api.CoreAPI;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.util.*;

public class Utilities {
    private static final NumberFormat COMMA_FORMAT = NumberFormat.getInstance();

    public static void runAsync(Runnable runnable) {
        new Thread(runnable).start();
    }

    public static HashMap<?, ?> sortByValue(Map<?, ?> map) {
        List<Map.Entry<?, ?>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, (o1, o2) -> ((Comparable) o2.getValue()).compareTo(o1.getValue()));
        HashMap<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static UUID generateNewUUID(String name, UUID premiumID) {
        return premiumID == null ? getCrackedUUIDFromName(name) : premiumID;
    }

    public static UUID getCrackedUUIDFromName(String name) {
        if (name == null) return null;
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    public static String readInput(InputStream inputStream) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        inputStream.close();
        return result.toString("UTF-8");
    }

    public static UUID fromUnDashedUUID(String id) {
        return id == null ? null : new UUID(
                new BigInteger(id.substring(0, 16), 16).longValue(),
                new BigInteger(id.substring(16, 32), 16).longValue()
        );
    }

    public static void delay(Runnable runnable, long delay) {
        new Thread(() -> {
            try {
                Thread.sleep(delay / 20 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            runnable.run();
        }).start();
    }

    public static String commaify(long l) {
        return COMMA_FORMAT.format(l);
    }

    public static String getRankFromPlayer(ProxiedPlayer player) {
        final Rank rank = CoreAPI.getPlayerRank(player.getUniqueId());
        return Utilities.trans(rank.getPrefix());
    }

    public static String trans(final String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    static {
        COMMA_FORMAT.setGroupingUsed(true);
    }
}
