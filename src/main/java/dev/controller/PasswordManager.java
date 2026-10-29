package dev.controller;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import dev.model.PasswordEntry;
import dev.model.PasswordHistoryItem;
import dev.persistence.Vault;
import dev.persistence.VaultException;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;

/**
 * Sesión de una bóveda: mantiene las entradas descifradas y la clave derivada (dentro de {@link Vault}).
 * No conserva la contraseña maestra tras {@link #unlock}.
 */
public class PasswordManager {
    private static final int MAX_HISTORY = 10;
    private static final Type LIST_TYPE = new TypeToken<ArrayList<PasswordEntry>>() {}.getType();

    private final Path vaultFile;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonSerializer<LocalDateTime>) (src, type, ctx) -> new JsonPrimitive(src.toString()))
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, ctx) -> LocalDateTime.parse(json.getAsString()))
            .create();

    private Vault vault;
    private List<PasswordEntry> entries = new ArrayList<>();

    public PasswordManager(Path vaultFile) {
        this.vaultFile = vaultFile;
    }

    /** Abre la bóveda. La contraseña no se modifica: quien la pasa debe borrarla. */
    public void unlock(char[] password) throws VaultException {
        Vault opened = Vault.open(vaultFile, password);
        try {
            List<PasswordEntry> loaded = gson.fromJson(new String(opened.data(), StandardCharsets.UTF_8), LIST_TYPE);
            entries = loaded != null ? loaded : new ArrayList<>();
        } catch (JsonParseException e) {
            opened.close();
            throw new VaultException("El contenido de la bóveda no es válido", e);
        }
        vault = opened;
    }

    public void saveData() throws VaultException {
        requireSession();
        vault.save(serialize());
    }

    public void lock() {
        if (vault != null) {
            vault.close();
            vault = null;
        }
        entries.clear();
    }

    /** Cambia la clave maestra (nueva sal incluida). Comprueba antes la actual. */
    public void changeMasterPassword(char[] oldPassword, char[] newPassword) throws VaultException {
        requireSession();
        Vault.open(vaultFile, oldPassword).close(); // lanza InvalidPassword si la actual no es correcta
        vault.changePassword(newPassword, serialize());
    }

    public List<PasswordEntry> getEntries() {
        return entries;
    }

    public void addEntry(PasswordEntry entry) {
        entry.setId(entries.stream().mapToInt(PasswordEntry::getId).max().orElse(0) + 1);
        entries.add(entry);
    }

    /** Sustituye la entrada con el mismo id. Si cambia la contraseña, la anterior pasa al historial. */
    public void updateEntry(PasswordEntry updated) {
        for (int i = 0; i < entries.size(); i++) {
            PasswordEntry previous = entries.get(i);
            if (previous.getId() != updated.getId()) continue;

            List<PasswordHistoryItem> history = new ArrayList<>(previous.getHistory());
            String oldPwd = previous.getPassword();
            if (oldPwd != null && !oldPwd.isEmpty() && !oldPwd.equals(updated.getPassword())) {
                history.add(0, new PasswordHistoryItem(oldPwd, LocalDateTime.now()));
            }
            updated.setHistory(history.subList(0, Math.min(MAX_HISTORY, history.size())));
            entries.set(i, updated);
            return;
        }
    }

    /**
     * Añade las entradas que no estén ya (mismo nombre, usuario y sitio, sin distinguir mayúsculas).
     *
     * @return cuántas se añadieron
     */
    public int importEntries(List<PasswordEntry> incoming) {
        int added = 0;
        for (PasswordEntry e : incoming) {
            boolean duplicate = entries.stream().anyMatch(x -> identity(x).equals(identity(e)));
            if (!duplicate) {
                addEntry(e);
                added++;
            }
        }
        return added;
    }

    private static List<String> identity(PasswordEntry e) {
        return java.util.stream.Stream.of(e.getTitle(), e.getUsername(), e.getUrl())
                .map(s -> Objects.toString(s, "").trim().toLowerCase(Locale.ROOT)).toList();
    }

    public Path getVaultFile() {
        return vaultFile;
    }

    public void deleteEntry(int id) {
        entries.removeIf(e -> e.getId() == id);
    }

    private byte[] serialize() {
        return gson.toJson(entries).getBytes(StandardCharsets.UTF_8);
    }

    private void requireSession() {
        if (vault == null) {
            throw new IllegalStateException("No hay sesión activa");
        }
    }
}
