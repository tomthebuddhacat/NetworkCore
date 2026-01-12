package com.zerosio.listeners;

import com.zerosio.Config;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.ProxyPingEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

public class MotdListener implements Listener {

    private final Plugin plugin;

    public MotdListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public void refresh() {
        Config.reload();
        plugin.getLogger().info("MOTD config reloaded.");
    }

    @EventHandler
    public void onPing(ProxyPingEvent event) {
        ServerPing ping = event.getResponse();
        PendingConnection connection = event.getConnection();

        boolean maintenance = Config.getBoolean("maintenance", false);

        if (maintenance) {
            String line1 = ChatColor.translateAlternateColorCodes('&',
                    "              &aAscent Network &c[1.8-1.20]");
            String line2 = ChatColor.translateAlternateColorCodes('&',
                    "   &cThe network is currently undergoing maintenance.");
            ping.setDescription(line1 + "\n" + line2);
            ping.setPlayers(new ServerPing.Players(0, 0, new ServerPing.PlayerInfo[0]));
            ping.setVersion(new ServerPing.Protocol("§cMaintenance", -1));
        } else {
            String line1 = ChatColor.translateAlternateColorCodes('&',
                    "&r&r                 &aAscent Network &c[1.8-1.20]&r&r");
            String line2 = ChatColor.translateAlternateColorCodes('&',
                    "&r&r                      &2&lRELEASE");
            ping.setDescription(line1 + "\n" + line2);
            ping.setPlayers(
                    new ServerPing.Players(1500, plugin.getProxy().getOnlineCount(), ping.getPlayers().getSample()));
            ping.setVersion(new ServerPing.Protocol("Requires MC 1.8 / 1.20.3", ping.getVersion().getProtocol()));
        }

        event.setResponse(ping);
    }
}