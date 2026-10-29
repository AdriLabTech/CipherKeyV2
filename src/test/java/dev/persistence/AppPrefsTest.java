package dev.persistence;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.prefs.Preferences;

import static org.junit.jupiter.api.Assertions.*;

class AppPrefsTest {
    private Preferences testNode;

    @BeforeEach
    void isolate() {
        testNode = Preferences.userRoot().node("dev/cipherkey-test-" + System.nanoTime());
        AppPrefs.useNode(testNode);
    }

    @AfterEach
    void cleanUp() throws Exception {
        testNode.removeNode();
    }

    @Test
    void recentVaultsAreDedupedOrderedAndCapped() {
        for (int i = 0; i < 10; i++) AppPrefs.addRecentVault(Paths.get("/tmp/v" + i + ".ckey"));
        AppPrefs.addRecentVault(Paths.get("/tmp/v5.ckey"));
        List<Path> recent = AppPrefs.recentVaults();
        assertEquals(6, recent.size());
        assertEquals(Paths.get("/tmp/v5.ckey"), recent.get(0));
        assertEquals(1, recent.stream().filter(p -> p.endsWith("v5.ckey")).count());
    }

    @Test
    void defaults() {
        assertFalse(AppPrefs.isDark());
        assertEquals(5, AppPrefs.autoLockMinutes());
        assertTrue(AppPrefs.recentVaults().isEmpty());
    }
}
