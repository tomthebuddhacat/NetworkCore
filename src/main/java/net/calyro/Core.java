package net.calyro;

import net.calyro.agent.AgentHttpServer;
import net.calyro.authentication.premium.PremiumProvider;
import net.calyro.commands.impl.CommandManager;
import net.calyro.database.DatabaseManager;
import net.calyro.database.MongoManager;
import net.calyro.database.User;
import net.calyro.guilds.Guild;
import net.calyro.listeners.AuthListener;
import net.calyro.listeners.ChatListener;
import net.calyro.listeners.MotdListener;
import net.calyro.listeners.PlayerListener;
import net.calyro.privacy.Encryptor;
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
