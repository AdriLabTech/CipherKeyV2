package dev.security;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class PasswordGenerator {
    private boolean includeUppercase = true;
    private boolean includeLowercase = true;
    private boolean includeDigits = true;
    private boolean includeSymbols = true;
    private int length = 16;

    private static final String UPPERCASE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String LOWERCASE = "abcdefghijklmnopqrstuvwxyz";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private final SecureRandom random = new SecureRandom();

    public String generate() {
        if (!includeUppercase && !includeLowercase && !includeDigits && !includeSymbols) {
            throw new IllegalArgumentException("Debe seleccionar al menos un tipo de carácter");
        }

        StringBuilder pool = new StringBuilder();
        if (includeUppercase) pool.append(UPPERCASE);
        if (includeLowercase) pool.append(LOWERCASE);
        if (includeDigits) pool.append(DIGITS);
        if (includeSymbols) pool.append(SYMBOLS);

        StringBuilder password = new StringBuilder(length);

        // Asegurar al menos un carácter de cada tipo seleccionado
        List<Character> mandatory = new ArrayList<>();
        if (includeUppercase) mandatory.add(UPPERCASE.charAt(random.nextInt(UPPERCASE.length())));
        if (includeLowercase) mandatory.add(LOWERCASE.charAt(random.nextInt(LOWERCASE.length())));
        if (includeDigits) mandatory.add(DIGITS.charAt(random.nextInt(DIGITS.length())));
        if (includeSymbols) mandatory.add(SYMBOLS.charAt(random.nextInt(SYMBOLS.length())));

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

    // Getters y setters
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
