package dev.security;

import dev.security.PasswordStrength.Level;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordStrengthTest {
    @Test
    void emptyAndCommonPasswordsAreVeryWeak() {
        assertEquals(Level.VERY_WEAK, PasswordStrength.evaluate("").level());
        assertEquals(Level.VERY_WEAK, PasswordStrength.evaluate("Password").level());
        assertEquals(Level.VERY_WEAK, PasswordStrength.evaluate("123456").level());
    }

    @Test
    void repetitionsAndSequencesDoNotInflateTheScore() {
        assertTrue(PasswordStrength.evaluate("aaaaaaaaaaaaaaaa").level().compareTo(Level.WEAK) <= 0);
        assertTrue(PasswordStrength.evaluate("abcdefghijklmnop").level().compareTo(Level.WEAK) <= 0);
    }

    @Test
    void longRandomPasswordIsVeryStrong() {
        assertEquals(Level.VERY_STRONG, PasswordStrength.evaluate("k9#Vq2!xLm7@Pz4$").level());
    }

    @Test
    void scoreGrowsWithLengthAndVariety() {
        double shortLower = PasswordStrength.evaluate("kqzmfx").bits();
        double longMixed = PasswordStrength.evaluate("kQz9mF!x2Lp#").bits();
        assertTrue(longMixed > shortLower);
    }

    @Test
    void generatedPasswordsOfDefaultLengthAreStrong() {
        PasswordGenerator g = new PasswordGenerator();
        for (int i = 0; i < 50; i++) {
            assertTrue(PasswordStrength.evaluate(g.generate()).level().compareTo(Level.STRONG) >= 0);
        }
    }
}
