package dev.controller;

import dev.model.PasswordEntry;
import dev.persistence.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordManagerTest {
    private static final char[] PWD = "clave de prueba larga".toCharArray();

    @TempDir
    Path dir;
    Path file;
    PasswordManager manager;

    @BeforeEach
    void setUp() throws Exception {
        file = dir.resolve("t.ckey");
        Vault.create(file, PWD).close();
        manager = new PasswordManager(file);
        manager.unlock(PWD);
    }

    private PasswordEntry add(String title, String password) {
        PasswordEntry e = new PasswordEntry(title, "u", password, "https://" + title + ".com", "");
        manager.addEntry(e);
        return e;
    }

    private PasswordEntry edited(PasswordEntry original, String newPassword) {
        PasswordEntry e = new PasswordEntry(original.getTitle(), "u", newPassword, original.getUrl(), "");
        e.setId(original.getId());
        return e;
    }

    @Test
    void changingThePasswordPushesTheOldOneToHistory() {
        PasswordEntry original = add("a", "old-1");
        manager.updateEntry(edited(original, "new-2"));
        manager.updateEntry(edited(manager.getEntries().get(0), "new-3"));

        List<String> history = manager.getEntries().get(0).getHistory().stream().map(h -> h.getPassword()).toList();
        assertEquals(List.of("new-2", "old-1"), history); // la más reciente primero
    }

    @Test
    void unchangedPasswordDoesNotTouchHistory() {
        PasswordEntry original = add("a", "same");
        manager.updateEntry(edited(original, "same"));
        assertTrue(manager.getEntries().get(0).getHistory().isEmpty());
    }

    @Test
    void historyIsCappedAtTen() {
        PasswordEntry first = add("a", "p0");
        for (int i = 1; i <= 15; i++) {
            manager.updateEntry(edited(manager.getEntries().get(0), "p" + i));
        }
        assertEquals(10, manager.getEntries().get(0).getHistory().size());
        assertEquals("p14", manager.getEntries().get(0).getHistory().get(0).getPassword());
    }

    @Test
    void historyAndTotpSurviveSaveAndReopen() throws Exception {
        PasswordEntry original = add("a", "old");
        PasswordEntry updated = edited(original, "new");
        updated.setTotpSecret("JBSWY3DPEHPK3PXP");
        manager.updateEntry(updated);
        manager.saveData();

        PasswordManager reopened = new PasswordManager(file);
        reopened.unlock(PWD);
        PasswordEntry e = reopened.getEntries().get(0);
        assertEquals("JBSWY3DPEHPK3PXP", e.getTotpSecret());
        assertEquals("old", e.getHistory().get(0).getPassword());
        assertNotNull(e.getHistory().get(0).getReplacedAt());
    }

    @Test
    void importSkipsDuplicatesIgnoringCase() {
        add("GitHub", "x");
        PasswordEntry dup = new PasswordEntry("github", "U", "other", "HTTPS://GITHUB.COM", "");
        PasswordEntry fresh = new PasswordEntry("Nuevo", "u", "p", "", "");

        int added = manager.importEntries(List.of(dup, fresh));

        assertEquals(1, added);
        assertEquals(2, manager.getEntries().size());
        assertNotEquals(manager.getEntries().get(0).getId(), manager.getEntries().get(1).getId());
    }

    @Test
    void vaultWrittenBeforeTheNewFieldsStillLoads() throws Exception {
        String legacyJson = "[{\"id\":1,\"title\":\"Viejo\",\"username\":\"u\",\"password\":\"p\",\"url\":\"\",\"notes\":\"\","
                + "\"created\":\"2026-01-01T10:00:00\",\"modified\":\"2026-01-01T10:00:00\"}]";
        Path legacy = dir.resolve("legacy.ckey");
        try (Vault v = Vault.create(legacy, PWD)) {
            v.save(legacyJson.getBytes(StandardCharsets.UTF_8));
        }
        PasswordManager m = new PasswordManager(legacy);
        m.unlock(PWD);
        PasswordEntry e = m.getEntries().get(0);
        assertTrue(e.getHistory().isEmpty());
        assertNull(e.getTotpSecret());
        assertNull(e.getCategory());
        assertFalse(e.isFavorite());
    }
}
