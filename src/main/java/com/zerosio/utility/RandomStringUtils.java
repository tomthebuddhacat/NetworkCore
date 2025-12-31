package com.zerosio.utility;

import java.security.SecureRandom;

public class RandomStringUtils {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String random(int length) {
        return random(length, ALPHANUMERIC);
    }

    public static String random(int length, String characters) {
        if (characters == null || characters.isEmpty()) {
            throw new IllegalArgumentException("Character set must not be empty");
        }

        StringBuilder result = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(characters.length());
            result.append(characters.charAt(index));
        }
        return result.toString();
    }
}
