package net.calyro.agent;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AgentHttpServer {

    private static final Gson GSON = new Gson();

    public static void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(7800), 0);

        server.createContext("/server/register", exchange -> {
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    sendJson(exchange, 405, Map.of(
                            "success", false,
                            "error", "METHOD_NOT_ALLOWED"
                    ));
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> data = GSON.fromJson(body, Map.class);

                String name = (String) data.get("name");
                String host = (String) data.get("host");
                String portNum = (String) data.get("port");

                if (name == null || host == null || portNum == null) {
                    sendJson(exchange, 400, Map.of(
                            "success", false,
                            "error", "INVALID_BODY"
                    ));
                    return;
                }

                if (ProxyServer.getInstance().getServers().containsKey(name)) {
                    sendJson(exchange, 409, Map.of(
                            "success", false,
                            "error", "ALREADY_EXISTS"
                    ));
                    return;
                }

                int port = Integer.parseInt((String) data.get("port"));

                ServerInfo info = ProxyServer.getInstance().constructServerInfo(
                        name,
                        new InetSocketAddress(host, port),
                        name,
                        false
                );

                ProxyServer.getInstance().getServers().put(name, info);

                ProxyServer.getInstance().getLogger().info(
                        "[Agent] Constructed & registered server " + name + " (" + host + ":" + port + ")"
                );

                sendJson(exchange, 200, Map.of("success", true));

            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, Map.of(
                        "success", false,
                        "error", "INTERNAL_ERROR"
                ));
            } finally {
                exchange.close();
            }
        });

        server.createContext("/server/unregister", exchange -> {
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    sendJson(exchange, 405, Map.of(
                            "success", false,
                            "error", "METHOD_NOT_ALLOWED"
                    ));
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> data = GSON.fromJson(body, Map.class);

                String name = (String) data.get("name");

                if (name == null || !ProxyServer.getInstance().getServers().containsKey(name)) {
                    sendJson(exchange, 404, Map.of(
                            "success", false,
                            "error", "NOT_FOUND"
                    ));
                    return;
                }

                ProxyServer.getInstance().getServers().remove(name);

                ProxyServer.getInstance().getLogger().info(
                        "[Agent] Unregistered (destructed) server " + name
                );

                sendJson(exchange, 200, Map.of("success", true));

            } catch (Exception e) {
                e.printStackTrace();
                sendJson(exchange, 500, Map.of(
                        "success", false,
                        "error", "INTERNAL_ERROR"
                ));
            } finally {
                exchange.close();
            }
        });

        server.start();
        ProxyServer.getInstance().getLogger().info("[Agent] HTTP server started on port 7800");
    }

    private static void sendJson(HttpExchange exchange, int status, Map<String, Object> body) {
        try {
            byte[] json = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, json.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(json);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}