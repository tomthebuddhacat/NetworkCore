package com.zerosio.listeners;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyPingEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import com.zerosio.Config;
import com.zerosio.Messages;
import net.kyori.adventure.text.Component;

public class MotdListener {

    private final ProxyServer proxyServer;

    public MotdListener(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    public void refresh() {
        Config.reload();
    }

    @Subscribe
    public void onPing(ProxyPingEvent proxyPingEvent) {
        ServerPing ping = proxyPingEvent.getPing();

        boolean maintenance = Config.getBoolean("maintenance", false);

        if (maintenance) {
            Component line1 = Messages.get("motd.maintenance.line-one");
            Component line2 = Messages.get("motd.maintenance.line-two");
            int onlinePlayers = Integer.parseInt(Config.getString("server.motd.maintenance.online-players", "0"));
            int maximumPlayers = Integer.parseInt(Config.getString("server.motd.maintenance.maximum-players", "0"));
            int version = Integer.parseInt(Config.getString("server.motd.maintenance.protocol-version", "0"));
            String motdName = Config.getString("server.motd.maintenance.protocol-name", "Maintenance");

            ping = ping.asBuilder()
                    .description(Component.text()
                            .append(line1)
                            .append(Component.newline())
                            .append(line2)
                            .build())
                    .onlinePlayers(onlinePlayers)
                    .maximumPlayers(maximumPlayers)
                    .version(new ServerPing.Version(version, motdName))
                    .build();
        } else {
            Component line1 = Messages.get("motd.released.line-one");
            Component line2 = Messages.get("motd.released.line-two");
            int maximumPlayers = Integer.parseInt(Config.getString("server.motd.released.maximum-players", "0"));
            String motdName = Config.getString("server.motd.released.protocol-name", "Requires MC 1.8 / 1.20.3");

            ping = ping.asBuilder()
                    .description(Component.text()
                            .append(line1)
                            .append(Component.newline())
                            .append(line2)
                            .build())
                    .onlinePlayers(proxyServer.getPlayerCount())
                    .maximumPlayers(maximumPlayers)
                    .version(new ServerPing.Version(ping.getVersion().getProtocol(), motdName))
                    .build();
        }
        proxyPingEvent.setPing(ping);
    }
}