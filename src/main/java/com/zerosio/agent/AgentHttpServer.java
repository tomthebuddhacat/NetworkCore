package com.zerosio.agent;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.zerosio.Core;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class AgentHttpServer {

    private static final Gson GSON = new Gson();

    public static void start() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(7800), 0);

        server.createContext("/server/register", exchange -> {
            try {
                if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("error", "METHOD_NOT_ALLOWED");

                    sendJson(exchange, 405, response);
                    return;
                }

                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Map<String, Object> data = GSON.fromJson(body, Map.class);

                String name = (String) data.get("name");
                String host = (String) data.get("host");
                String portNum = (String) data.get("port");

                if (name == null || host == null || portNum == null) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("error", "INVALID_BODY");

                    sendJson(exchange, 400, response);
                    return;
                }

                if (Core.getInstance().getProxy().getServer(name).isPresent()) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", false);
                    response.put("error", "ALREADY_EXISTS");

                    sendJson(exchange, 409, response);
                    return;
                }

                int port = Integer.parseInt((String) data.get("port"));

                ServerInfo serverInfo = new ServerInfo(name, new InetSocketAddress(host, port));

                Core.getInstance().getProxy().registerServer(serverInfo);

                System.out.println("[Agent] Constructed & registered server " + name + " (" + host + ":" + port + ")");

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

                if (name == null || !Core.getInstance().getProxy().getServer(name).isPresent()) {
                    sendJson(exchange, 404, Map.of(
                            "success", false,
                            "error", "NOT_FOUND"
                    ));
                    return;
                }

                Core.getInstance().getProxy().getServer(name).ifPresent(registeredServer -> Core.getInstance().getProxy().unregisterServer(registeredServer.getServerInfo()));
                System.out.println("[Agent] Unregistered (destructed) server " + name);

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
        System.out.println("[Agent] HTTP server started on port 7800");
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