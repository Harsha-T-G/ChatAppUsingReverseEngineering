package com.harsha.minichat.common;

import java.security.SecureRandom;

/**
 * Generates ids in the form REA observed in the recorded URLs: 26 lowercase letters and digits.
 */
public final class Ids {

	private static final char[] ALPHABET = "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
	private static final int LENGTH = 26;
	private static final SecureRandom RANDOM = new SecureRandom();

	private Ids() {
	}

	public static String newId() {
		char[] out = new char[LENGTH];
		for (int i = 0; i < LENGTH; i++) {
			out[i] = ALPHABET[RANDOM.nextInt(ALPHABET.length)];
		}
		return new String(out);
	}
}
