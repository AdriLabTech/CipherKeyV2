package dev.security;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PasswordGenerator {
    private boolean includeUppercase = true;
    private boolean includeLowercase = true;
    private boolean includeDigits = true;
    private boolean includeSymbols = true;
    private boolean excludeAmbiguous = false;
    private int length = 16;

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private final SecureRandom random = new SecureRandom();

    private static final String AMBIGUOUS = "Il1O0|";

    public String generate() {
        if (!includeUppercase && !includeLowercase && !includeDigits && !includeSymbols) {
            throw new IllegalArgumentException("Debe seleccionar al menos un tipo de carácter");
        }

        String upper = pick(UPPERCASE), lower = pick(LOWERCASE), digits = pick(DIGITS), symbols = pick(SYMBOLS);
        StringBuilder pool = new StringBuilder();
        if (includeUppercase) pool.append(upper);
        if (includeLowercase) pool.append(lower);
        if (includeDigits) pool.append(digits);
        if (includeSymbols) pool.append(symbols);

        StringBuilder password = new StringBuilder(length);

        // Asegurar al menos un carácter de cada tipo seleccionado
        List<Character> mandatory = new ArrayList<>();
        if (includeUppercase) mandatory.add(upper.charAt(random.nextInt(upper.length())));
        if (includeLowercase) mandatory.add(lower.charAt(random.nextInt(lower.length())));
        if (includeDigits) mandatory.add(digits.charAt(random.nextInt(digits.length())));
        if (includeSymbols) mandatory.add(symbols.charAt(random.nextInt(symbols.length())));

        for (char c : mandatory) {
            password.append(c);
        }

        // Rellenar el resto
        for (int i = mandatory.size(); i < length; i++) {
            password.append(pool.charAt(random.nextInt(pool.length())));
        }

        // Fisher-Yates con SecureRandom
        char[] pwdArray = password.toString().toCharArray();
        for (int i = pwdArray.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char tmp = pwdArray[i];
            pwdArray[i] = pwdArray[j];
            pwdArray[j] = tmp;
        }

        return new String(pwdArray);
    }

    private String pick(String chars) {
        if (!excludeAmbiguous) return chars;
        StringBuilder sb = new StringBuilder();
        for (char c : chars.toCharArray()) {
            if (AMBIGUOUS.indexOf(c) < 0) sb.append(c);
        }
        return sb.toString();
    }

    // Getters y setters
    public boolean isExcludeAmbiguous() { return excludeAmbiguous; }
    public void setExcludeAmbiguous(boolean excludeAmbiguous) { this.excludeAmbiguous = excludeAmbiguous; }
    public boolean isIncludeUppercase() { return includeUppercase; }
    public void setIncludeUppercase(boolean includeUppercase) { this.includeUppercase = includeUppercase; }
    public boolean isIncludeLowercase() { return includeLowercase; }
    public void setIncludeLowercase(boolean includeLowercase) { this.includeLowercase = includeLowercase; }
    public boolean isIncludeDigits() { return includeDigits; }
    public void setIncludeDigits(boolean includeDigits) { this.includeDigits = includeDigits; }
    public boolean isIncludeSymbols() { return includeSymbols; }
    public void setIncludeSymbols(boolean includeSymbols) { this.includeSymbols = includeSymbols; }
    public int getLength() { return length; }
    public void setLength(int length) { this.length = length; }
}
