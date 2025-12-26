package net.calyro.utility;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class Json {

	public static String toJson(Object obj) {
		if (obj instanceof Map) return mapToJson((Map<?, ?>) obj);
		if (obj instanceof Iterable) return listToJson((Iterable<?>) obj);
		return pojoToJson(obj);
	}

	private static String listToJson(Iterable<?> list) {
		StringBuilder sb = new StringBuilder("[");
		int i = 0;
		for (Object o : list) {
			if (i++ > 0) sb.append(",");
			sb.append(toJson(o)); // <-- serialize each
		}
		sb.append("]");
		return sb.toString();
	}

	private static String mapToJson(Map<?, ?> map) {
		StringBuilder sb = new StringBuilder("{");
		int i = 0;
		for (Entry<?, ?> e : map.entrySet()) {
			if (i++ > 0) sb.append(",");
			sb.append("\"").append(e.getKey()).append("\":");
			sb.append("\"").append(e.getValue()).append("\"");
		}
		sb.append("}");
		return sb.toString();
	}

	private static String pojoToJson(Object obj) {
		StringBuilder sb = new StringBuilder("{");
		Field[] fields = obj.getClass().getDeclaredFields();
		int i = 0;
		try {
			for (Field f : fields) {
				f.setAccessible(true);
				if (i++ > 0) sb.append(",");
				sb.append("\"").append(f.getName()).append("\":");
				sb.append("\"").append(f.get(obj)).append("\"");
			}
		} catch (Exception ignored) {}
		sb.append("}");
		return sb.toString();
	}

	public static <T> T fromJson(String json, Class<T> clazz) {
		try {
			if (Map.class.isAssignableFrom(clazz)) {
				return (T) parseObjectToMap(json);
			}

			T instance = clazz.getDeclaredConstructor().newInstance();

			json = json.trim();
			if (json.startsWith("{")) json = json.substring(1);
			if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

			if (json.isEmpty())
				return instance;

			String[] parts = json.split(",");

			for (String part : parts) {
				String[] kv = part.split(":", 2);
				if (kv.length != 2) continue;

				String key = kv[0].replace("\"", "").trim();
				String raw = kv[1].replace("\"", "").trim();

				Field f;
				try {
					f = clazz.getDeclaredField(key);
				} catch (NoSuchFieldException e) {
					// Unknown field -> ignore
					continue;
				}
				
				f.setAccessible(true);

				Class<?> type = f.getType();

				Object value;

				if (type == String.class) {
					value = raw;
				} else if (type == int.class || type == Integer.class) {
					value = Integer.parseInt(raw);
				} else if (type == long.class || type == Long.class) {
					value = Long.parseLong(raw);
				} else if (type == boolean.class || type == Boolean.class) {
					value = Boolean.parseBoolean(raw);
				} else if (type.isEnum()) {
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Enum<?> e = Enum.valueOf((Class<? extends Enum>) type, raw);
					value = e;
				} else {
					continue;
				}

				f.set(instance, value);
			}

			return instance;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static Map<String, Object> parseObjectToMap(String json) {
		Map<String, Object> map = new HashMap<>();

		json = json.trim();
		if (json.startsWith("{")) json = json.substring(1);
		if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

		if (json.isEmpty()) return map;

		String[] parts = json.split(",");

		for (String part : parts) {
			String[] kv = part.split(":", 2);
			if (kv.length != 2) continue;

			String key = kv[0].trim().replace("\"", "");
			String val = kv[1].trim();

			Object parsed;
			if (val.startsWith("\"")) {
				parsed = val.replace("\"", "");
			} else if (val.equals("true") || val.equals("false")) {
				parsed = Boolean.parseBoolean(val);
			} else {
				try {
					parsed = Integer.parseInt(val);
				} catch (NumberFormatException e) {
					parsed = val;
				}
			}

			map.put(key, parsed);
		}

		return map;
	}
}