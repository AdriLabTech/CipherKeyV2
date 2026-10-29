package dev.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa una entrada individual dentro de la bóveda de contraseñas.
 * Cada entrada tiene un identificador único, título, nombre de usuario,
 * contraseña, URL, notas, y marcas de tiempo de creación y última modificación.
 */
public class PasswordEntry {
    private int id;
    private String title;
    private String username;
    private String password;
    private String url;
    private String notes;
    private LocalDateTime created;
    private LocalDateTime modified;

    /**
     * Constructor por defecto. Inicializa las marcas de tiempo con la hora actual.
     */
    public PasswordEntry() {
        this.created = LocalDateTime.now();
        this.modified = this.created;
    }

    /**
     * Constructor con campos principales. Inicializa las marcas de tiempo con la hora actual.
     *
     * @param title    Título de la entrada
     * @param username Nombre de usuario
     * @param password Contraseña
     * @param url      URL asociada (opcional)
     * @param notes    Notas adicionales (opcional)
     */
    public PasswordEntry(String title, String username, String password, String url, String notes) {
        this.title = title;
        this.username = username;
        this.password = password;
        this.url = url;
        this.notes = notes;
        this.created = LocalDateTime.now();
        this.modified = this.created;
    }

    // Getters y setters

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.modified = LocalDateTime.now();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
        this.modified = LocalDateTime.now();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
        this.modified = LocalDateTime.now();
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.modified = LocalDateTime.now();
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
        this.modified = LocalDateTime.now();
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public void setCreated(LocalDateTime created) {
        this.created = created;
    }

    public LocalDateTime getModified() {
        return modified;
    }

    public void setModified(LocalDateTime modified) {
        this.modified = modified;
    }

    /**
     * Actualiza explícitamente la marca de tiempo de modificación al instante actual.
     * Útil cuando se modifican varios campos de forma externa.
     */
    public void touch() {
        this.modified = LocalDateTime.now();
    }

    // equals y hashCode basados en el identificador único

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PasswordEntry that = (PasswordEntry) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "PasswordEntry{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", username='" + username + '\'' +
                ", url='" + url + '\'' +
                ", modified=" + modified +
                '}';
    }
}