package net.calyro.listeners;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.AsyncEvent;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;
import net.calyro.Core;
import net.calyro.api.ControllerAPI;
import net.calyro.authentication.AuthDB;
import net.calyro.authentication.Authentication;
import net.calyro.authentication.PreLoginResult;
import net.calyro.authentication.premium.PremiumException;
import net.calyro.authentication.premium.PremiumUser;
import net.calyro.database.User;

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

public class AuthListener implements Listener {

    private static final Pattern NAME_PATTERN = Pattern.compile("[a-zA-Z0-9_]*");
    private static final ForkJoinPool ASYNC_POOL = new ForkJoinPool(4);

    @EventHandler(priority = EventPriority.LOWEST)
    public void onProfileRequest(LoginEvent event) {
        String username = event.getConnection().getName();
        User profile = User.getUser(username);
        PendingConnection connection = event.getConnection();

        if (profile == null) {
            try {
                profile = checkAndValidateByName(username, null, true, event.getConnection().getAddress().getAddress());
            } catch (Exception exception) {
                Core.getInstance().getLogger().severe("Failed to create user profile for " + username + " during LoginEvent: " + exception.getMessage());
                event.setCancelled(true);
                return;
            }
        }

        try {
            setField(connection, "uniqueId", profile.getUuid(), true);
            setField(connection, "rewriteId", profile.getUuid(), false);
        } catch (NoSuchFieldException exception) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onSeberrConnoct(ServerConnectEvent event) {
        if (event.getReason() != ServerConnectEvent.Reason.JOIN_PROXY) {
			return;
		}
		
        processPostLogin(event);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(PreLoginEvent event) {
        runAsyncEvent(event, () -> {
            PreLoginResult result = processPreLogin(event.getConnection().getName(), event.getConnection().getAddress().getAddress());

            switch (result.getState()) {
                case DENIED:
                    assert result.getMessage() != null;
                    event.setCancelled(true);
                    event.setCancelReason(TextComponent.fromLegacyText(result.getMessage()));
                    break;
                case FORCE_ONLINE: event.getConnection().setOnlineMode(true); break;
                case FORCE_OFFLINE: event.getConnection().setOnlineMode(false); break;
            }
        });
    }

    public void runAsyncEvent(AsyncEvent<?> event, Runnable runnable) {
        event.registerIntent(Core.getInstance());

        ASYNC_POOL.execute(() -> {
            try {
                runnable.run();
            } finally {
                event.completeIntent(Core.getInstance());
            }
        });
    }

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

    private PreLoginResult processPreLogin(String username, InetAddress address) {
        if (username.length() > 16 || !NAME_PATTERN.matcher(username).matches()) {
            return new PreLoginResult(PreLoginResult.PreLoginState.DENIED, "§cYour username contains illegal characters!", null);
        }

        PremiumUser mojangData;

        try {
            mojangData = Core.getInstance().getPremiumProvider().getUserForName(username);
        } catch (PremiumException exception) {
            String message;
            if (Objects.requireNonNull(exception.getIssue()) == PremiumException.Issue.THROTTLED) {
                message = "§cThe authentication servers are currently being rate limited. Please try again later.";
            } else {
                Core.getInstance().getLogger().severe("Encountered an exception while communicating with the Mojang API!");
                exception.printStackTrace(System.err);
                message = "§cAn error occurred while trying to verify your account. Please try again later.";
            }

            return new PreLoginResult(PreLoginResult.PreLoginState.DENIED, message, null);
        }

        if (mojangData == null) {
            User user;
            try {
                user = checkAndValidateByName(username, null, true, address);
            } catch (NoSuchElementException exception) {
                return new PreLoginResult(PreLoginResult.PreLoginState.DENIED, "§cAn error occurred while trying to register your account. Please try again later.", null);
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
                    return new PreLoginResult(PreLoginResult.PreLoginState.DENIED, "§cAn error occurred while trying to register your account. Please try again later.", null);
                }

                return new PreLoginResult(PreLoginResult.PreLoginState.FORCE_ONLINE, null, userByName);
            } else {
                User byName;
                try {
                    byName = checkAndValidateByName(username, mojangData, false, address);
                } catch (Exception exception) {
                    exception.printStackTrace();
                    return new PreLoginResult(PreLoginResult.PreLoginState.DENIED, "§cAn error occurred while trying to validate your account. Please try again later.", null);
                }

                if (byName != null && !user.retrieveLastKnownName().equals(byName.retrieveLastKnownName())) {
                    return new PreLoginResult(PreLoginResult.PreLoginState.DENIED, "§cThe username \"" + username + "\" is already in use!", null);
                }

                if (!mojangData.isReliable()) {
                    Core.getInstance().getLogger().warning("User " + username + " has probably changed their name. Data returned from Mojang API is not reliable, faking a new one using the current nickname.");
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

    public static boolean processPostLogin(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        UUID uuid = player.getUniqueId();
        User user = User.getUser(uuid);

        Duration sessionTime = Duration.ofSeconds(604800);

        if (AuthDB.getPremiumUUID(user) != null) {
            event.setTarget(ControllerAPI.getRandomAvailableInstanceServerInfo("lobby"));
            ProxyServer.getInstance().getScheduler().schedule(Core.getInstance(), () -> {
                player.sendMessage("§aYou have been automatically logged in as you are a premium user.");
            }, sessionTime.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } else if (Authentication.isIPAuthenticated(player)) {
            event.setTarget(ControllerAPI.getRandomAvailableInstanceServerInfo("lobby"));
            ProxyServer.getInstance().getScheduler().schedule(Core.getInstance(), () -> {
                player.sendMessage("§aYou have been automatically logged in as your session is still valid.");
            }, sessionTime.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } else {
            if (AuthDB.isRegistered(player.getUniqueId())) {
                Authentication.login(event);
                return false;
            } else {
                Authentication.register(event);
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
            Core.getInstance().getLogger().warning(
                    "Premium data for " + username + " is not reliable; switching to offline registration."
            );
        }

        user.save();
        return user;
    }


}
