package net.calyro.agent;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BackendManager {

    private static final Map<String, BackendInstance> BACKENDS =
            new ConcurrentHashMap<>();

    public static void register(BackendInstance backend) {
        ProxyServer proxy = ProxyServer.getInstance();

        ServerInfo info = proxy.constructServerInfo(
                backend.getName(),
                new InetSocketAddress(backend.getHost(), backend.getPort()),
                "Dynamic backend",
                false
        );

        proxy.getServers().put(backend.getName(), info);
        BACKENDS.put(backend.getName(), backend);
    }

    public static void unregister(String name) {
        ProxyServer proxy = ProxyServer.getInstance();
        proxy.getServers().remove(name);
        BACKENDS.remove(name);
    }

    public static boolean exists(String name) {
        return BACKENDS.containsKey(name);
    }
}
