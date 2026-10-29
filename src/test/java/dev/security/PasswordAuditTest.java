package dev.security;

import dev.model.PasswordEntry;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PasswordAuditTest {
    private static PasswordEntry entry(String title, String password, LocalDateTime modified) {
        PasswordEntry e = new PasswordEntry(title, "u", password, "", "");
        e.setModified(modified);
        return e;
    }

    @Test
    void detectsWeakReusedOldAndEmpty() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 12, 0);
        LocalDateTime recent = now.minusDays(10);
        List<PasswordEntry> entries = List.of(
                entry("Débil", "123456", recent),
                entry("A", "k9#Vq2!xLm7@Pz4$", recent),
                entry("B", "k9#Vq2!xLm7@Pz4$", recent),
                entry("Vieja", "Zx8$mN2#qWe5!rT9", now.minusDays(400)),
                entry("Vacía", "", recent),
                entry("Buena", "Pp9&kL3@vB6#nQ1zRt", recent));

        PasswordAudit.Report r = PasswordAudit.analyze(entries, now, PasswordAudit.DEFAULT_OLD_DAYS);

        assertEquals(List.of("Débil"), r.weak().stream().map(f -> f.entry().getTitle()).toList());
        assertEquals(List.of("A", "B"), r.reused().stream().map(f -> f.entry().getTitle()).toList());
        assertTrue(r.reused().get(0).detail().contains("B"));
        assertEquals(List.of("Vieja"), r.old().stream().map(f -> f.entry().getTitle()).toList());
        assertEquals(List.of("Vacía"), r.empty().stream().map(f -> f.entry().getTitle()).toList());
        assertFalse(r.isClean());
        assertEquals(5, r.total());
    }

    @Test
    void cleanVaultHasNoFindings() {
        LocalDateTime now = LocalDateTime.of(2026, 10, 29, 12, 0);
        PasswordAudit.Report r = PasswordAudit.analyze(
                List.of(entry("Buena", "Pp9&kL3@vB6#nQ1zRt", now.minusDays(5))), now, 365);
        assertTrue(r.isClean());
    }

    @Test
    void emptyPasswordsAreNotCountedAsReused() {
        LocalDateTime now = LocalDateTime.now();
        PasswordAudit.Report r = PasswordAudit.analyze(List.of(entry("A", "", now), entry("B", "", now)), now, 365);
        assertTrue(r.reused().isEmpty());
        assertEquals(2, r.empty().size());
    }
}
