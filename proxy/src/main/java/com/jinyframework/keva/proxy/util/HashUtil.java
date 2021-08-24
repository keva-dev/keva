package com.jinyframework.keva.proxy.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author tuna
 */
public class HashUtil {
	private static MessageDigest instance = null;

	static {
		try {
			instance = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			//Do nothing
		}
	}

	private HashUtil() {
	}

	public static long hash(String value) {
		instance.reset();
		instance.update(value.getBytes());
		byte[] digest = instance.digest();

		long h = 0;
		for (int i = 0; i < 4; i++) {
			h <<= 8;
			h |= ((int) digest[i]) & 0xFF;
		}
		return h;
	}
}
