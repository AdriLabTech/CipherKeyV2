package dev.model;

import java.time.LocalDateTime;

/** Contraseña anterior de una entrada y el momento en que dejó de usarse. */
public class PasswordHistoryItem {
    private String password;
    private LocalDateTime replacedAt;

    public PasswordHistoryItem() {}

    public PasswordHistoryItem(String password, LocalDateTime replacedAt) {
        this.password = password;
        this.replacedAt = replacedAt;
    }

    public String getPassword() {
        return password;
    }

    public LocalDateTime getReplacedAt() {
        return replacedAt;
    }
}
