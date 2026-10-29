package dev.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {
    @Test
    void respectsLengthAndIncludesEverySelectedClass() {
        PasswordGenerator g = new PasswordGenerator();
        for (int len = 4; len <= 64; len += 5) {
            g.setLength(len);
            for (int i = 0; i < 50; i++) {
                String p = g.generate();
                assertEquals(len, p.length());
                assertTrue(p.chars().anyMatch(Character::isUpperCase), p);
                assertTrue(p.chars().anyMatch(Character::isLowerCase), p);
                assertTrue(p.chars().anyMatch(Character::isDigit), p);
                assertTrue(p.chars().anyMatch(c -> "!@#$%^&*()_+-=[]{}|;:,.<>?".indexOf(c) >= 0), p);
            }
        }
    }

    @Test
    void usesOnlySelectedClasses() {
        PasswordGenerator g = new PasswordGenerator();
        g.setIncludeUppercase(false);
        g.setIncludeLowercase(false);
        g.setIncludeSymbols(false);
        g.setLength(32);
        assertTrue(g.generate().chars().allMatch(Character::isDigit));
    }

    @Test
    void rejectsEmptySelection() {
        PasswordGenerator g = new PasswordGenerator();
        g.setIncludeUppercase(false);
        g.setIncludeLowercase(false);
        g.setIncludeDigits(false);
        g.setIncludeSymbols(false);
        assertThrows(IllegalArgumentException.class, g::generate);
    }

    @Test
    void generatesDifferentPasswords() {
        PasswordGenerator g = new PasswordGenerator();
        assertNotEquals(g.generate(), g.generate());
    }
}
