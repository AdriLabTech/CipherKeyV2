package dev.security;

import java.util.Set;

/**
 * Estimación sencilla de la fuerza de una contraseña (entropía aproximada con penalizaciones).
 * Es una guía para el usuario, no una garantía: no sustituye a un estimador con diccionarios (zxcvbn).
 */
public final class PasswordStrength {
    public enum Level {
        VERY_WEAK("Muy débil"), WEAK("Débil"), FAIR("Aceptable"), STRONG("Fuerte"), VERY_STRONG("Muy fuerte");

        private final String label;

        Level(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public record Result(double bits, Level level) {}

    private static final Set<String> COMMON = Set.of(
            "password", "contraseña", "contrasena", "123456", "12345678", "123456789", "qwerty", "qwertyuiop",
            "abc123", "111111", "letmein", "iloveyou", "admin", "welcome", "monkey", "dragon", "passw0rd",
            "password1", "123123", "000000", "1q2w3e4r");

    private PasswordStrength() {}

    public static Result evaluate(char[] password) {
        return evaluate(new String(password));
    }

    public static Result evaluate(String password) {
        if (password == null || password.isEmpty()) {
            return new Result(0, Level.VERY_WEAK);
        }
        if (COMMON.contains(password.toLowerCase())) {
            return new Result(5, Level.VERY_WEAK);
        }

        int pool = 0;
        boolean lower = false, upper = false, digit = false, symbol = false;
        for (char c : password.toCharArray()) {
            if (Character.isLowerCase(c)) lower = true;
            else if (Character.isUpperCase(c)) upper = true;
            else if (Character.isDigit(c)) digit = true;
            else symbol = true;
        }
        if (lower) pool += 26;
        if (upper) pool += 26;
        if (digit) pool += 10;
        if (symbol) pool += 33;

        double bits = effectiveLength(password) * (Math.log(pool) / Math.log(2));
        return new Result(bits, levelFor(bits));
    }

    /** Cuenta como un carácter cada símbolo "nuevo": repeticiones y secuencias (aaa, abc, 123) casi no suman. */
    private static double effectiveLength(String s) {
        double len = 0;
        for (int i = 0; i < s.length(); i++) {
            if (i == 0) {
                len += 1;
                continue;
            }
            int diff = s.charAt(i) - s.charAt(i - 1);
            len += (diff == 0 || Math.abs(diff) == 1) ? 0.2 : 1;
        }
        return len;
    }

    private static Level levelFor(double bits) {
        if (bits < 28) return Level.VERY_WEAK;
        if (bits < 40) return Level.WEAK;
        if (bits < 60) return Level.FAIR;
        if (bits < 80) return Level.STRONG;
        return Level.VERY_STRONG;
    }
}
