package com.zerosio.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.server.ServerInfo;
import com.zerosio.Core;
import com.zerosio.api.request.SendRequest;
import com.zerosio.instance.AvailableInstance;
import com.zerosio.instance.InstanceSelector;
import com.zerosio.utility.Json;

public class ControllerAPI {
	
	public static ServerInfo getRandomAvailableInstanceServerInfo(String template) {
		return Core.getInstance().getProxy().getServer(getRandomAvailableInstance(template).getName())
				.map(RegisteredServer::getServerInfo).orElse(null);
	}

	public static RegisteredServer getRandomAvailableInstanceServer(String type) {
		List<RegisteredServer> registeredServers = Core.getInstance().getProxy().getAllServers().stream()
				.filter(registeredServer -> registeredServer.getServerInfo().getName().startsWith(type))
				.collect(Collectors.toList());

		if (registeredServers.isEmpty()) {
			return null;
		}

		return registeredServers.get(ThreadLocalRandom.current().nextInt(registeredServers.size()));
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