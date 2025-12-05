package net.calyro.mojang;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

import net.calyro.mojang.data.PremiumData;

public class MojangAPI {
    private static final String API_URL = "https://api.mojang.com/users/profiles/minecraft/";

    public PremiumData getPremiumUUID(String username) {
        try {
            URL url = new URL(API_URL + username);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(3000);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
                reader.close();
                String json = response.toString();
                if (!json.isEmpty() && !json.equals("null")) {
                    UUID uuid = parseUUID(json.split("\"id\":\"")[1].split("\"")[0]);
                    return new PremiumData(true, uuid);
                }
            }
            return new PremiumData(false, null);
        } catch (Exception e) {
            return new PremiumData(false, null);
        }
    }

    private UUID parseUUID(String raw) {
        return UUID.fromString(raw.replaceFirst(
            "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]+)",
            "$1-$2-$3-$4-$5"
        ));
    }
}
