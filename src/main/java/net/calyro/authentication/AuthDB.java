package net.calyro.authentication;

import net.calyro.database.User;
import net.calyro.privacy.Encryptor;
import org.bson.Document;

import java.time.Instant;
import java.util.UUID;

public class AuthDB {

    private static Document getAuthDocument(User user) {
        @SuppressWarnings("unchecked")
        Document auth = user.getData("authentication");
        if (auth == null) {
            auth = new Document()
                    .append("isPremium", false)
                    .append("password", "")
                    .append("isRegistered", false)
                    .append("registeredAt", -1L);
            user.setData("authentication", auth);
        }
        return auth;
    }

    public static UUID getPremiumUUID(User user) {
        Document auth = getAuthDocument(user);
        String uuidStr = auth.getString("premiumUuid");
        if (uuidStr == null || uuidStr.isEmpty()) {
            return null;
        }
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean register(UUID uuid, String plainPassword) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);

        if (auth.getBoolean("isRegistered", false)) return false;

        Encryptor enc = new Encryptor();
        String encrypted = enc.encrypt(plainPassword);

        auth.put("password", encrypted);
        auth.put("isRegistered", true);
        auth.put("registeredAt", Instant.now().toEpochMilli());

        user.setData("authentication", auth);
        AuthDB.setLastSessionValidation(uuid, Instant.now().toEpochMilli());
        return true;
    }

    public static boolean isRegistered(UUID uuid) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);
        return auth.getBoolean("isRegistered", false);
    }

    public static boolean checkPassword(UUID uuid, String plainPassword) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);

        String stored = auth.getString("password");
        if (stored == null || stored.isEmpty()) return false;

        Encryptor enc = new Encryptor();
        String decrypted = enc.decrypt(stored);

        return plainPassword.equals(decrypted);
    }

    public static boolean setPassword(UUID uuid, String newPlainPassword) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);

        Encryptor enc = new Encryptor();
        String encrypted = enc.encrypt(newPlainPassword);

        auth.put("password", encrypted);
        if (!auth.getBoolean("isRegistered", false)) {
            auth.put("isRegistered", true);
            auth.put("registeredAt", Instant.now().toEpochMilli());
        }
        user.setData("authentication", auth);
        return true;
    }

    public static long getLastSessionValidation(UUID uuid) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);
        return auth.getLong("lastSessionValidation") != null ? auth.getLong("lastSessionValidation") : -1L;
    }

    public static void setPremiumUUID(User user, UUID premiumUUID) {
        Document auth = getAuthDocument(user);
        auth.put("premiumUuid", premiumUUID.toString());
        user.setData("authentication", auth);
    }

    public static void setLastSessionValidation(UUID uuid, long timestamp) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);
        auth.put("lastSessionValidation", timestamp);
        user.setData("authentication", auth);
    }

    public static void setPremium(UUID uuid, boolean premium) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);
        auth.put("isPremium", premium);
        user.setData("authentication", auth);
    }

    public static boolean isPremium(UUID uuid) {
        User user = User.getUser(uuid);
        Document auth = getAuthDocument(user);
        return getPremiumUUID(user) != null;
    }

    public static Document fetchAuth(UUID uuid) {
        User user = User.getUser(uuid);
        return getAuthDocument(user);
    }
}
