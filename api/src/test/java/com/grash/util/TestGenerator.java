package com.grash.util;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;

public class TestGenerator {
    public static String generateString() {
        return UUID.randomUUID().toString();
    }

    public static int generateRandomInt(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        int min = (int) Math.pow(10, length - 1); // Smallest number with the desired length
        int max = (int) Math.pow(10, length) - 1; // Largest number with the desired length

        Random random = new Random();
        return random.nextInt((max - min) + 1) + min;
    }

    public static int generateRandomIntBetween(int low, int high) {
        Random r = new Random();
        return r.nextInt(high - low) + low;
    }

    public static double generateRandomDoubleBetween(double low, double high) {
        Random r = new Random();
        return low + (high - low) * r.nextDouble();
    }


    public static String generatePhone() {
        return String.valueOf(generateRandomInt(8));
    }

    public static BigDecimal generateMoneyAmount() {
        return BigDecimal.valueOf(generateRandomInt(4));
    }

    public static String generateEmail() {
        return generateEightCharString() + "@" + generateEightCharString() + ".com";
    }

    public static String generateEightCharString() {
        StringBuilder returnValue = new StringBuilder(8);
        for (int i = 0; i < 8; i++) {
            String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            returnValue.append(ALPHABET.charAt(new SecureRandom().nextInt(ALPHABET.length())));
        }
        return returnValue.toString();
    }
}
