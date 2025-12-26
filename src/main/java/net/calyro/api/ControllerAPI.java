package net.calyro.api;

import java.util.ArrayList;
import java.util.List;

import net.calyro.api.request.SendRequest;
import net.calyro.instance.AvailableInstance;
import net.calyro.instance.InstanceSelector;
import net.calyro.utility.Json;import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

public class ControllerAPI {
	
	public static ServerInfo getRandomAvailableInstanceServerInfo(String template) {
		return ProxyServer.getInstance().getServerInfo(getRandomAvailableInstance(template).getName());
	}
	
	public static AvailableInstance getRandomAvailableInstance(String template) {
		return InstanceSelector.pickRandomJoinable(ControllerAPI.getAvailableInstances(template));
	}
	
	public static List<AvailableInstance> getAvailableInstances(String template) {
		return parseAvailable(getAvailable(template));
	}
	
	private static List<AvailableInstance> parseAvailable(String json) {
		List<AvailableInstance> list = new ArrayList<>();

		if (json == null || json.isEmpty())
			return list;

		json = json.trim();
		if (!json.startsWith("[") || !json.endsWith("]"))
			return list;

		json = json.substring(1, json.length() - 1).trim();
		if (json.isEmpty())
			return list;

		String[] objects = json.split("\\},\\{");

		for (String obj : objects) {
			if (!obj.startsWith("{")) obj = "{" + obj;
			if (!obj.endsWith("}")) obj = obj + "}";

			AvailableInstance instance =
				Json.fromJson(obj, AvailableInstance.class);

			if (instance != null && instance.getName() != null) {
				list.add(instance);
			}
		}

		return list;
	}
	
	private static String getAvailable(String template) {
		try {
			String host = "127.0.0.1";
			int port = 7000;

			String res = SendRequest.get(
							 "http://" + host + ":" + port + "/instances/available?template=" + template
						 );

			return res;
		} catch (Exception e) {
			return "";
		}
	}
}