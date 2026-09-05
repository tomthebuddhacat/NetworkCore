package com.zerosio.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.PostLoginEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.zerosio.Core;
import com.zerosio.api.ControllerAPI;
import com.zerosio.authentication.AuthDB;
import com.zerosio.authentication.Authentication;
import com.zerosio.authentication.PreLoginResult;
import com.zerosio.authentication.premium.PremiumException;
import com.zerosio.authentication.premium.PremiumUser;
import com.zerosio.database.User;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.*;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import static com.zerosio.authentication.PreLoginResult.PreLoginState.*;

public class AuthListener {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]*");
    private static final ForkJoinPool ASYNC_POOL = new ForkJoinPool(4);

    @Subscribe
    public void onProfileRequest(LoginEvent event) {
        String username = event.getPlayer().getUsername();
        User profile = User.getUser(username);

        if (profile == null) {
            try {
                profile = checkAndValidateByName(username, null, true, event.getPlayer().getRemoteAddress().getAddress());
            } catch (Exception exception) {
                System.err.println("Failed to create user profile for " + username + " during LoginEvent: " + exception.getMessage());
                event.setResult(ResultedEvent.ComponentResult.denied(Component.text("An error occurred while creating your profile.", NamedTextColor.RED)));
                return;
            }
        }
    }

    @Subscribe
    public void onServerConnected(PostLoginEvent postLoginEvent) {
        processPostLogin(postLoginEvent.getPlayer());
    }

    @Subscribe
    public void onPreLogin(PreLoginEvent event) {
        runAsyncEvent(event, () -> {
            PreLoginResult preLoginResult = processPreLogin(event.getUsername(), event.getConnection().getRemoteAddress().getAddress());

            switch (preLoginResult.getState()) {
                case DENIED:
                    event.setResult(PreLoginEvent.PreLoginComponentResult.denied(Component.text(preLoginResult.getMessage())));
                    break;
                case FORCE_ONLINE:
                    break;
                case FORCE_OFFLINE:
                    break;
            }
        });
    }

    public void runAsyncEvent(PreLoginEvent event, Runnable runnable) {
        ASYNC_POOL.execute(() -> {
            try {
                runnable.run();
            } catch (Exception exception) {
                System.err.println("Error while processing PreLoginEvent: " + exception.getMessage());
            }
        });
    }

    /* Removed? Might be unnecessary for Velocity
    private void setField(PendingConnection connection, String fieldName, Object value, boolean failOnNotFound) throws NoSuchFieldException {
        Class<?> clazz = connection.getClass();
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(connection, value);
        } catch (NoSuchFieldException exception) {
            if (!failOnNotFound) return;
            Logger logger = Core.getInstance().getLogger();
            logger.severe("The " + fieldName + " field was not found in the PendingConnection class, please report this to the developer. And attach the class summary below.");
            logger.severe("-- BEGIN CLASS SUMMARY --");
            logger.severe("Class: " + clazz.getName());
            for (Field field : clazz.getDeclaredFields()) {
                logger.severe(field.getType().getName() + ": " + field.getName());
            }
            logger.severe("-- END CLASS SUMMARY --");
            throw exception;
        } catch (IllegalAccessException exception) {
            throw new RuntimeException(exception);
        }
    }
     */

    private PreLoginResult processPreLogin(String username, InetAddress address) {
        if (username.length() > 16 || !NAME_PATTERN.matcher(username).matches()) {
            return new PreLoginResult(DENIED, "§cYour username contains illegal characters!", null);
        }

        PremiumUser mojangData;

        try {
            mojangData = Core.getInstance().getPremiumProvider().getUserForName(username);
        } catch (PremiumException exception) {
            String message;
            if (Objects.requireNonNull(exception.getIssue()) == PremiumException.Issue.THROTTLED) {
                message = "The authentication servers are currently being rate limited. Please try again later.";
            } else {
                System.err.println("Encountered an exception while communicating with the Mojang API!");
                message = "An error occurred while trying to verify your account. Please try again later.";
            }

            return new PreLoginResult(DENIED, message, null);
        }

        if (mojangData == null) {
            User user;
            try {
                user = checkAndValidateByName(username, null, true, address);
            } catch (NoSuchElementException exception) {
                return new PreLoginResult(DENIED, "§cAn error occurred while trying to register your account. Please try again later.", null);
            }

            if (AuthDB.getPremiumUUID(user) != null) {
                return new PreLoginResult(PreLoginResult.PreLoginState.FORCE_ONLINE, null, user);
            }
        } else {
            UUID premiumID = mojangData.getUniqueId();
            User user = User.getByPremiumUUID(premiumID);

            if (user == null) {
                User userByName;
                try {
                    userByName = checkAndValidateByName(username, mojangData, true, address);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return new PreLoginResult(DENIED, "§cAn error occurred while trying to register your account. Please try again later.", null);
                }

                return new PreLoginResult(PreLoginResult.PreLoginState.FORCE_ONLINE, null, userByName);
            } else {
                User byName;
                try {
                    byName = checkAndValidateByName(username, mojangData, false, address);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return new PreLoginResult(DENIED, "§cAn error occurred while trying to validate your account. Please try again later.", null);
                }

                if (byName != null && !user.retrieveLastKnownName().equals(byName.retrieveLastKnownName())) {
                    return new PreLoginResult(DENIED, "§cThe username \"" + username + "\" is already in use!", null);
                }

                if (!mojangData.isReliable()) {
                    System.err.println("User " + username + " has probably changed their name. Data returned from Mojang API is not reliable, faking a new one using the current nickname.");
                    mojangData = new PremiumUser(mojangData.getUniqueId(), username, false);
                }

                if (!user.retrieveLastKnownName().contentEquals(mojangData.getName())) {
                    user.setLastKnownName(mojangData.getName());
                    user.save();
                }

                return new PreLoginResult(PreLoginResult.PreLoginState.FORCE_ONLINE, null, user);
            }
        }

        return new PreLoginResult(PreLoginResult.PreLoginState.FORCE_OFFLINE, null, null);
    }

    public static boolean processPostLogin(Player player) {
        UUID uuid = player.getUniqueId();
        User user = User.getUser(uuid);

        Duration sessionTime = Duration.ofSeconds(604800);

        if (AuthDB.getPremiumUUID(user) != null) {
            RegisteredServer lobby = ControllerAPI.getRandomAvailableInstanceServer("lobby");

            if (lobby != null) {
                player.createConnectionRequest(lobby).fireAndForget();
            }

            Core.getInstance().getProxy().getScheduler()
                    .buildTask(Core.getInstance(), () -> {
                        player.sendMessage(Component.text("You have been automatically logged in as you are a premium user.", NamedTextColor.RED));
                    }).delay(sessionTime).schedule();

            return true;

        } else if (Authentication.isIPAuthenticated(player)) {
            RegisteredServer lobby = ControllerAPI.getRandomAvailableInstanceServer("lobby");

            if (lobby != null) {
                player.createConnectionRequest(lobby).fireAndForget();
            }

            Core.getInstance().getProxy().getScheduler()
                    .buildTask(Core.getInstance(), () -> {
                        player.sendMessage(Component.text("You have been automatically logged in as your session is still valid.", NamedTextColor.RED));
                    }).delay(sessionTime.toMillis(), TimeUnit.MILLISECONDS).schedule();

            return true;

        } else {
            if (AuthDB.isRegistered(player.getUniqueId())) {
                Authentication.login(player);
                return false;
            } else {
                Authentication.register(player);
                return false;
            }
        }
    }

    private User checkAndValidateByName(String username, PremiumUser premiumUser, boolean generate, InetAddress ip) {
        User existingUser = User.getUser(username);

        if (existingUser != null) {
            if (!existingUser.retrieveLastKnownName().contentEquals(username)) {
                throw new RuntimeException("The username \"" + username + "\" is already in use!");
            }

            return User.getUser(username, premiumUser != null ? premiumUser.getUniqueId() : null);
        }

        if (!generate) return null;

        UUID newUUID = premiumUser != null ? premiumUser.getUniqueId() : UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8));

        if (User.existsUserInDatabase(newUUID)) {
            User existingUserWithUUID = User.getUser(newUUID);
            if (existingUserWithUUID != null) {
                String lastKnownName = existingUserWithUUID.retrieveLastKnownName();

                if (lastKnownName.contentEquals(username)) {
                    existingUserWithUUID.setData("last_login", System.currentTimeMillis());
                    existingUserWithUUID.save();
                    return existingUserWithUUID;
                } else if (lastKnownName.equals("Unknown")) {
                    existingUserWithUUID.setLastKnownName(username);
                    existingUserWithUUID.setData("last_login", System.currentTimeMillis());
                    existingUserWithUUID.save();
                    return existingUserWithUUID;
                } else {
                    throw new RuntimeException("The UUID \"" + newUUID + "\" is already in use by another user!");
                }
            }
        }

        User user = User.getUser(username, premiumUser != null ? premiumUser.getUniqueId() : null);
        user.setData("last_login", System.currentTimeMillis());

        if (premiumUser != null && premiumUser.isReliable()) {
            if (!premiumUser.getName().contentEquals(username)) {
                throw new RuntimeException("The username \"" + username + "\" does not match the premium data!");
            }

            user.setLastKnownName(premiumUser.getName());
            AuthDB.setPremiumUUID(user, premiumUser.getUniqueId());
        } else if (premiumUser != null && !premiumUser.isReliable()) {
            System.err.println("Premium data for " + username + " is not reliable; switching to offline registration.");
        }

        user.save();
        return user;
    }


}
