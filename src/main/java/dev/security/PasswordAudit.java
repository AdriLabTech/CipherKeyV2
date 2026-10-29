package dev.security;

import dev.model.PasswordEntry;
import dev.security.PasswordStrength.Level;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

/** Revisión de la bóveda: contraseñas débiles, repetidas, antiguas o vacías. */
public final class PasswordAudit {
    public static final int DEFAULT_OLD_DAYS = 365;

    /** Una entrada con un problema y una explicación corta para mostrar al usuario. */
    public record Finding(PasswordEntry entry, String detail) {}

    public record Report(List<Finding> weak, List<Finding> reused, List<Finding> old, List<Finding> empty) {
        public boolean isClean() {
            return weak.isEmpty() && reused.isEmpty() && old.isEmpty() && empty.isEmpty();
        }

        public int total() {
            return weak.size() + reused.size() + old.size() + empty.size();
        }
    }

    private PasswordAudit() {}

    public static Report analyze(List<PasswordEntry> entries, LocalDateTime now, int oldDays) {
        List<Finding> weak = new ArrayList<>(), reused = new ArrayList<>(), old = new ArrayList<>(), empty = new ArrayList<>();
        Map<String, List<PasswordEntry>> byPassword = new LinkedHashMap<>();

        for (PasswordEntry e : entries) {
            String pwd = e.getPassword();
            if (pwd == null || pwd.isEmpty()) {
                empty.add(new Finding(e, "Sin contraseña"));
                continue;
            }
            byPassword.computeIfAbsent(pwd, k -> new ArrayList<>()).add(e);

            PasswordStrength.Result r = PasswordStrength.evaluate(pwd);
            if (r.level().compareTo(Level.WEAK) <= 0) {
                weak.add(new Finding(e, r.level().label() + " · ~" + Math.round(r.bits()) + " bits"));
            }
            long days = Duration.between(e.getModified(), now).toDays();
            if (days >= oldDays) {
                old.add(new Finding(e, "Sin cambios desde hace " + days + " días"));
            }
        }

        for (List<PasswordEntry> group : byPassword.values()) {
            if (group.size() < 2) continue;
            for (PasswordEntry e : group) {
                String others = String.join(", ", group.stream().filter(o -> o != e).map(PasswordEntry::getTitle).toList());
                reused.add(new Finding(e, "Misma contraseña que: " + others));
            }
        }
        return new Report(weak, reused, old, empty);
    }
}
