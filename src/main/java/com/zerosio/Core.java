package com.zerosio;

import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.zerosio.agent.AgentHttpServer;
import com.zerosio.authentication.premium.PremiumProvider;
import com.zerosio.commands.impl.CommandManager;
import com.zerosio.database.DatabaseManager;
import com.zerosio.database.MongoManager;
import com.zerosio.database.User;
import com.zerosio.guilds.Guild;
import com.zerosio.guilds.database.GuildDatabase;
import com.zerosio.listeners.AuthListener;
import com.zerosio.listeners.ChatListener;
import com.zerosio.listeners.MotdListener;
import com.zerosio.listeners.PlayerListener;
import com.zerosio.privacy.Encryptor;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "NetworkCore", name = "Network Core", version = "0.1.1-Velocity", description = "Hypixel's Proxy recreation attempt in Velocity.")
public class Core {

	private static Core instance;

	private static MotdListener motdListener;
	private PremiumProvider premiumProvider;

	private final ProxyServer proxyServer;
	private final Logger logger;

	private final Path dataDirectory;

	@Inject
	public Core(ProxyServer proxyServer, Logger logger, @DataDirectory Path dataDirectory) {
		this.proxyServer = proxyServer;
		this.logger = logger;
		this.dataDirectory = dataDirectory;

		instance = this;
	}

	@Subscribe
	public void onProxyInitialization(ProxyInitializeEvent proxyInitializeEvent) {
		logger.info("Starting NetworkCore...");

		Config.init();

		User.connect();
		MongoManager.connect();
		DatabaseManager.init();

		Guild.register();
		GuildDatabase.ensureCollectionn();

		motdListener = new MotdListener(this);
		registerListeners();

		CommandManager.registerCommands(this);

		new Encryptor();

		this.premiumProvider = new PremiumProvider();

		try {
			AgentHttpServer.start();
			logger.info("Starting HTTP Server...");
		} catch (Exception exception) {
			logger.error("Failed to start HTTP Server", exception);
		}

		logger.info("Successfully booted-up NetworkCore");
	}

	@Subscribe
	public void onProxyShutdown(ProxyShutdownEvent proxyShutdownEvent) {
		logger.info("Shutting down NetworkCore....");

		try {
			User.connect();
		} catch (Exception exception) {
			logger.error("Failed to disconnect User Database", exception);
		}

		logger.info("Successfully disabled NetworkCore");
	}

	private void registerListeners() {
		proxyServer.getEventManager().register(this, new PlayerListener());
		proxyServer.getEventManager().register(this, new ChatListener());
		proxyServer.getEventManager().register(this, motdListener);
		proxyServer.getEventManager().register(this, new AuthListener());
	}

	public static MotdListener getMotdListener() {
		return motdListener;
	}

	public static Core getInstance() {
		return instance;
	}

	public PremiumProvider getPremiumProvider() {
		return premiumProvider;
	}

	public Path getDataDirectory() {
		return dataDirectory;
	}
}

