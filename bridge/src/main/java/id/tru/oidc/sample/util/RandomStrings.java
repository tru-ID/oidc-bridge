package id.tru.oidc.sample.util;

import java.security.SecureRandom;

public class RandomStrings {
    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890-";
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String ofLength(int length) {
        var sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            var nextChar = CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length()));
            sb.append(nextChar);
        }

        return sb.toString();
    }

    private RandomStrings() {
    }
}
