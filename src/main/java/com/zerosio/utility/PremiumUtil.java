package com.zerosio.utility;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class PremiumUtil {

    public static boolean isPremium(ProxiedPlayer player) {
        try {
            String name = player.getName();

            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return false; // not a premium player
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            conn.disconnect();

            String mojangId = json.get("id").getAsString();
            UUID mojangUUID = fromMojangId(mojangId);

            return mojangUUID.equals(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }
    
    public static boolean isPremium(String name, UUID uuid) {
        try {

            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + name);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() != 200) {
                conn.disconnect();
                return false; // not a premium player
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            reader.close();
            conn.disconnect();

            String mojangId = json.get("id").getAsString();
            UUID mojangUUID = fromMojangId(mojangId);

            return mojangUUID.equals(uuid);
        } catch (Exception e) {
            return false;
        }
    }

    private static UUID fromMojangId(String id) {
        return UUID.fromString(
                id.replaceFirst(
                        "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)",
                        "$1-$2-$3-$4-$5"
                )
        );
    }
}
