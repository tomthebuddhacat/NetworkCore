package net.calyro.authentication.premium;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.calyro.Core;
import net.calyro.utility.ThrowableFunction;
import net.calyro.utility.Utilities;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class PremiumProvider {

    private final Cache<String, Optional<PremiumUser>> userCache;
    private final Gson gson = new Gson();
    private final List<ThrowableFunction<String, PremiumUser, PremiumException>> fetchers;

    public PremiumProvider() {
        userCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build();

        fetchers = new ArrayList<>();
        fetchers.add(this::getUserFromMojang);
        fetchers.add(this::getUserFromPlayerDB);
        fetchers.add(this::getUserFromMinetools);
    }

    public PremiumUser getUserForName(String name) throws PremiumException {
        name = name.toLowerCase();

        final PremiumException[] exceptionToThrow = new PremiumException[1];
        final String finalName = name;

        try {
            Optional<PremiumUser> result = userCache.get(name, () -> {
                for (int i = 0; i < fetchers.size(); i++) {
                    ThrowableFunction<String, PremiumUser, PremiumException> fetcher = fetchers.get(i);

                    try {
                        PremiumUser user = fetcher.apply(finalName);
                        if (user != null) {
                            return Optional.of(user);
                        }
                    } catch (PremiumException e) {
                        if (i == fetchers.size() - 1) {
                            exceptionToThrow[0] = e;
                        } else if (e.getIssue() == PremiumException.Issue.SERVER_EXCEPTION) {
                            throw new RuntimeException("Server exception while fetching premium user " + finalName, e);
                        } else if (e.getIssue() == PremiumException.Issue.THROTTLED) {
                            Core.getInstance().getLogger().warning(
                                    "Got throttled while fetching premium user. Falling back to an alternative API. Player's information might not be up-to-date."
                            );
                        } else {
                            Core.getInstance().getLogger().warning(
                                    "Got undefined exception while fetching premium user. Falling back to an alternative API. Player's information might not be up-to-date."
                            );
                        }
                    } catch (RuntimeException e) {
                        e.printStackTrace(System.err);
                        if (i == fetchers.size() - 1) {
                            exceptionToThrow[0] = new PremiumException(PremiumException.Issue.UNDEFINED, e);
                        }
                    }
                }
                return Optional.empty();
            });

            if (exceptionToThrow[0] != null) {
                throw exceptionToThrow[0];
            }

            return result.orElse(null);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof PremiumException) {
                throw (PremiumException) e.getCause();
            } else {
                throw new PremiumException(PremiumException.Issue.UNDEFINED, e);
            }
        }
    }

    private PremiumUser getUserFromPlayerDB(String name) throws PremiumException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://playerdb.co/api/player/minecraft/" + name).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            switch (connection.getResponseCode()) {
                case 200:
                    JsonObject data = gson.fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);

                    String id = data.get("data").getAsJsonObject().get("player").getAsJsonObject().get("id").getAsString();
                    String username = data.get("data").getAsJsonObject().get("player").getAsJsonObject().get("username").getAsString();

                    return new PremiumUser(
                            UUID.fromString(id),
                            username,
                            username.equalsIgnoreCase(name)
                    );
                case 400: return null;
                case 500: throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, Utilities.readInput(connection.getErrorStream()));
                default: throw new PremiumException(PremiumException.Issue.UNDEFINED, Utilities.readInput(connection.getErrorStream()));
            }
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, e);
        }
    }

    private PremiumUser getUserFromMinetools(String name) throws PremiumException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.minetools.eu/uuid/" + name).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            switch (connection.getResponseCode()) {
                case 200:
                    JsonObject data = gson.fromJson(new InputStreamReader(connection.getInputStream()), JsonObject.class);

                    JsonElement rawId = data.get("id");
                    if (rawId == null || rawId.isJsonNull()) {
                        JsonElement error = data.get("error");
                        if (error == null) {
                            return null;
                        }

                        String errorMessage = error.getAsString();
                        if (errorMessage.equals("Invalid UUID or nickname.")) {
                            return null;
                        } else {
                            throw new PremiumException(PremiumException.Issue.UNDEFINED, errorMessage);
                        }
                    }

                    String username = data.get("name").getAsString();
                    return new PremiumUser(
                            Utilities.fromUnDashedUUID(rawId.getAsString()),
                            username,
                            username.equalsIgnoreCase(name)
                    );
                case 400: return null;
                case 500: throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, Utilities.readInput(connection.getErrorStream()));
                default: throw new PremiumException(PremiumException.Issue.UNDEFINED, Utilities.readInput(connection.getErrorStream()));
            }
        } catch (SocketTimeoutException te) {
            throw new PremiumException(PremiumException.Issue.THROTTLED, "Minetools API timed out");
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.SERVER_EXCEPTION, e);
        }
    }

    private PremiumUser getUserFromMojang(String name) throws PremiumException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.mojang.com/users/profiles/minecraft/" + name).openConnection();
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int responseCode = connection.getResponseCode();
            switch (responseCode) {
                case 429:
                    throw new PremiumException(
                            PremiumException.Issue.THROTTLED,
                            Utilities.readInput(connection.getErrorStream())
                    );
                case 204:
                case 404:
                    return null;
                case 200:
                    JsonObject data = gson.fromJson(
                            new InputStreamReader(connection.getInputStream()),
                            JsonObject.class
                    );

                    String id = data.get("id").getAsString();
                    JsonElement demo = data.get("demo");

                    if (demo != null) {
                        return null;
                    } else {
                        return new PremiumUser(
                                Utilities.fromUnDashedUUID(id),
                                data.get("name").getAsString(),
                                true
                        );
                    }
                case 403:
                    if ("text/html".equals(connection.getContentType())) {
                        throw new PremiumException(
                                PremiumException.Issue.SERVER_EXCEPTION,
                                Utilities.readInput(connection.getErrorStream())
                        );
                    }
                    throw new PremiumException(
                            PremiumException.Issue.UNDEFINED,
                            Utilities.readInput(connection.getErrorStream())
                    );
                case 500:
                    throw new PremiumException(
                            PremiumException.Issue.SERVER_EXCEPTION,
                            Utilities.readInput(connection.getErrorStream())
                    );
                default:
                    throw new PremiumException(
                            PremiumException.Issue.UNDEFINED,
                            Utilities.readInput(connection.getErrorStream())
                    );
            }
        } catch (SocketTimeoutException te) {
            throw new PremiumException(PremiumException.Issue.THROTTLED, "Mojang API timed out");
        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.UNDEFINED, e);
        }
    }

    public PremiumUser getUserForUUID(UUID uuid) throws PremiumException {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString()).openConnection();

            int responseCode = connection.getResponseCode();
            switch (responseCode) {
                case 429:
                    throw new PremiumException(
                            PremiumException.Issue.THROTTLED,
                            Utilities.readInput(connection.getErrorStream())
                    );
                case 204:
                case 404:
                    return null;
                case 200: {
                    JsonObject data = gson.fromJson(
                            new InputStreamReader(connection.getInputStream()),
                            JsonObject.class
                    );

                    String name = data.get("name").getAsString();

                    return new PremiumUser(uuid, name, true); // Mojang API is always authoritative
                }
                case 500:
                    throw new PremiumException(
                            PremiumException.Issue.SERVER_EXCEPTION,
                            Utilities.readInput(connection.getErrorStream())
                    );
                default:
                    throw new PremiumException(
                            PremiumException.Issue.UNDEFINED,
                            Utilities.readInput(connection.getErrorStream())
                    );
            }

        } catch (IOException e) {
            throw new PremiumException(PremiumException.Issue.UNDEFINED, e);
        }
    }

}
