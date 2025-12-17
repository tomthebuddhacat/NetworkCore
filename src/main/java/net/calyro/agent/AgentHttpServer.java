package net.calyro.agent;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import net.calyro.Config;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AgentHttpServer {

	private static final Gson GSON = new Gson();

	public static void start() throws Exception {
		HttpServer server = HttpServer.create(
								new InetSocketAddress((int) Config.get("agent_port")), 0
							);

		server.createContext("/backend/register", exchange -> {
			if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
				sendJson(exchange, 405, Map.of(
							 "success", false,
							 "error", "METHOD_NOT_ALLOWED"
						 ));
				return;
			}

			try {
				Map<String, Object> data = GSON.fromJson(
					new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
					Map.class
				);

				String name = (String) data.get("name");
				String host = (String) data.get("host");
				int port = ((Number) data.get("port")).intValue();

				if (name == null || host == null) {
					sendJson(exchange, 400, Map.of(
								 "success", false,
								 "error", "INVALID_BODY"
							 ));
					return;
				}

				if (BackendManager.exists(name)) {
					sendJson(exchange, 409, Map.of(
								 "success", false,
								 "error", "ALREADY_REGISTERED"
							 ));
					return;
				}

				BackendManager.register(new BackendInstance(name, host, port));

				sendJson(exchange, 200, Map.of(
							 "success", true
						 ));
			    System.out.println("Registered Instance '" + name + "'");
			} catch (Exception e) {
				sendJson(exchange, 500, Map.of(
							 "success", false,
							 "error", "INTERNAL_ERROR"
						 ));
			}
		});

		server.createContext("/backend/unregister", exchange -> {
			if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
				sendJson(exchange, 405, Map.of(
							 "success", false,
							 "error", "METHOD_NOT_ALLOWED"
						 ));
				return;
			}

			try {
				Map<String, Object> data = GSON.fromJson(
					new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
					Map.class
				);

				String name = (String) data.get("name");

				if (name == null || !BackendManager.exists(name)) {
					sendJson(exchange, 404, Map.of(
								 "success", false,
								 "error", "NOT_FOUND"
							 ));
					return;
				}

				BackendManager.unregister(name);

				sendJson(exchange, 200, Map.of(
							 "success", true
						 ));
			    System.out.println("Removed Instance '" + name + "'");
			} catch (Exception e) {
				sendJson(exchange, 500, Map.of(
							 "success", false,
							 "error", "INTERNAL_ERROR"
						 ));
			}
		});

		server.start();
	}

	private static void sendJson(HttpExchange exchange, int status, Map<String, Object> body) {
		try {
			byte[] json = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(status, json.length);

			try (OutputStream os = exchange.getResponseBody()) {
				os.write(json);
			}
		} catch (Exception ignored) {
			try {
				exchange.sendResponseHeaders(500, -1);
			} catch (Exception ignored2) {
			}
		}
	}
}
