package dev.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Códigos TOTP (RFC 6238) compatibles con Google Authenticator, Aegis, etc. */
public final class Totp {
    private final byte[] secret;
    private final String algorithm;
    private final int digits;
    private final int period;

    private Totp(byte[] secret, String algorithm, int digits, int period) {
        this.secret = secret;
        this.algorithm = algorithm;
        this.digits = digits;
        this.period = period;
    }

    /**
     * Acepta un secreto base32 (con o sin espacios/guiones) o un enlace {@code otpauth://totp/...}
     * con {@code secret}, y opcionalmente {@code algorithm}, {@code digits} y {@code period}.
     *
     * @throws IllegalArgumentException si el texto no es válido
     */
    public static Totp parse(String input) {
        if (input == null || input.isBlank()) {
            throw new IllegalArgumentException("Clave 2FA vacía");
        }
        String text = input.trim();
        if (!text.toLowerCase(Locale.ROOT).startsWith("otpauth://")) {
            return new Totp(base32Decode(text), "HmacSHA1", 6, 30);
        }

        URI uri = URI.create(text);
        if (!"totp".equalsIgnoreCase(uri.getHost())) {
            throw new IllegalArgumentException("Solo se admiten enlaces otpauth://totp");
        }
        Map<String, String> q = new HashMap<>();
        String query = uri.getRawQuery();
        if (query != null) {
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    q.put(pair.substring(0, eq).toLowerCase(Locale.ROOT),
                            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }
        }
        if (!q.containsKey("secret")) {
            throw new IllegalArgumentException("El enlace no contiene secret");
        }
        String algo = switch (q.getOrDefault("algorithm", "SHA1").toUpperCase(Locale.ROOT)) {
            case "SHA1" -> "HmacSHA1";
            case "SHA256" -> "HmacSHA256";
            case "SHA512" -> "HmacSHA512";
            default -> throw new IllegalArgumentException("Algoritmo no soportado");
        };
        int digits = parseInt(q.getOrDefault("digits", "6"), 6, 8);
        int period = parseInt(q.getOrDefault("period", "30"), 1, 300);
        return new Totp(base32Decode(q.get("secret")), algo, digits, period);
    }

    private static int parseInt(String value, int min, int max) {
        try {
            int v = Integer.parseInt(value);
            if (v >= min && v <= max) return v;
        } catch (NumberFormatException ignored) {
            // cae al error de abajo
        }
        throw new IllegalArgumentException("Parámetro fuera de rango: " + value);
    }

    public String code(Instant now) {
        long counter = now.getEpochSecond() / period;
        try {
            Mac mac = Mac.getInstance(algorithm);
            mac.init(new SecretKeySpec(secret, algorithm));
            byte[] hash = mac.doFinal(ByteBuffer.allocate(8).putLong(counter).array());
            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24) | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8) | (hash[offset + 3] & 0xFF);
            long mod = 1;
            for (int i = 0; i < digits; i++) mod *= 10;
            return String.format("%0" + digits + "d", binary % mod);
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC no disponible", e);
        }
    }

    public int secondsRemaining(Instant now) {
        return period - (int) (now.getEpochSecond() % period);
    }

    public int period() {
        return period;
    }

    private static byte[] base32Decode(String text) {
        String s = text.replaceAll("[\\s-]", "").replaceAll("=+$", "").toUpperCase(Locale.ROOT);
        if (s.isEmpty()) throw new IllegalArgumentException("Clave 2FA vacía");
        byte[] out = new byte[s.length() * 5 / 8];
        int buffer = 0, bits = 0, index = 0;
        for (char c : s.toCharArray()) {
            int v = c >= 'A' && c <= 'Z' ? c - 'A' : c >= '2' && c <= '7' ? c - '2' + 26 : -1;
            if (v < 0) throw new IllegalArgumentException("La clave 2FA debe estar en base32 (A-Z, 2-7)");
            buffer = (buffer << 5) | v;
            bits += 5;
            if (bits >= 8) {
                out[index++] = (byte) (buffer >> (bits - 8));
                bits -= 8;
            }
        }
        return out;
    }
}
