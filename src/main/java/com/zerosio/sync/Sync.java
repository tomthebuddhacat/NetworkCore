package com.zerosio.sync;

import java.util.*;
import java.util.concurrent.*;

public class Sync {

	private static final Map<UUID, String> codeMap = new ConcurrentHashMap<>();
	private static final Map<String, UUID> reverseMap = new ConcurrentHashMap<>();
	private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
	private static final long EXPIRY_TIME_SECONDS = 300;
	private static final Random random = new Random();

	public static String generateCode(UUID uuid) {
		String code = randomCode();
		codeMap.put(uuid, code);
		reverseMap.put(code, uuid);
		scheduler.schedule(() -> {
			codeMap.remove(uuid);
			reverseMap.remove(code);
		}, EXPIRY_TIME_SECONDS, TimeUnit.SECONDS);
		return code;
	}

	public static UUID getUUIDFromCode(String code) {
		return reverseMap.get(code);
	}

	public static boolean isCodeValid(UUID uuid, String code) {
		return code.equals(codeMap.get(uuid));
	}

	public static void completeSync(UUID uuid) {
		String code = codeMap.remove(uuid);
		if (code != null)
			reverseMap.remove(code);
	}

	public static Map<String, UUID> getAllActiveCodes() {
		return new HashMap<>(reverseMap);
	}

	private static String randomCode() {
		String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 6; i++) {
			sb.append(chars.charAt(random.nextInt(chars.length())));
		}
		return sb.toString();
	}
}
