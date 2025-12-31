package com.zerosio;

import com.zerosio.agent.AgentHttpServer;
import com.zerosio.authentication.premium.PremiumProvider;
import com.zerosio.commands.impl.CommandManager;
import com.zerosio.database.DatabaseManager;
import com.zerosio.database.MongoManager;
import com.zerosio.database.User;
import com.zerosio.guilds.Guild;
import com.zerosio.listeners.AuthListener;
import com.zerosio.listeners.ChatListener;
import com.zerosio.listeners.MotdListener;
import com.zerosio.listeners.PlayerListener;
import com.zerosio.privacy.Encryptor;
import net.md_5.bungee.api.plugin.Plugin;

public class Core extends Plugin {

	private static Core instance;

	private static MotdListener motdListener;
	private PremiumProvider premiumProvider;

	@Override
	public void onEnable() {
		instance = this;

		Config.reload();
		User.connect();
		MongoManager.connect();
		DatabaseManager.init();
		Guild.register();

		motdListener = new MotdListener(this);

		registerListeners();
		CommandManager.registerCommands(this);
		new Encryptor();

		this.premiumProvider = new PremiumProvider();
		
		try {
		AgentHttpServer.start();
		getLogger().info("Started HTTP Server");
		} catch (Exception e) {}
		
		getLogger().info("NetworkCore has been enabled!");
	}

	@Override
	public void onDisable() {
		User.disconnect();
		getLogger().info("NetworkCore has been disabled!");
	}

	private void registerListeners() {
		getProxy().getPluginManager().registerListener(this, new PlayerListener());
		getProxy().getPluginManager().registerListener(this, new ChatListener());
		getProxy().getPluginManager().registerListener(this, motdListener);
		getProxy().getPluginManager().registerListener(this, new AuthListener());
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
}
