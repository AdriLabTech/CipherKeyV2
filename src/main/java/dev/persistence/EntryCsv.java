package dev.persistence;

import dev.model.PasswordEntry;

import java.util.*;

/**
 * Importa y exporta entradas en CSV. La importación se guía por los nombres de columna, así que sirve para
 * Bitwarden, Chrome/Edge/Firefox y KeePass(XC) sin elegir formato. La exportación usa el formato de Bitwarden.
 */
public final class EntryCsv {
    public record Imported(List<PasswordEntry> entries, int skipped) {}

    private static final List<String> EXPORT_HEADER = List.of("folder", "favorite", "type", "name", "notes", "fields",
            "reprompt", "login_uri", "login_username", "login_password", "login_totp");

    private static final Map<String, List<String>> SYNONYMS = Map.of(
            "title", List.of("name", "title", "account", "entry"),
            "username", List.of("login_username", "username", "user", "login", "user name"),
            "password", List.of("login_password", "password", "pass"),
            "url", List.of("login_uri", "url", "web site", "website", "uri", "login_url"),
            "notes", List.of("notes", "note", "comments", "comment"),
            "category", List.of("folder", "group", "grouping", "category"),
            "favorite", List.of("favorite"),
            "totp", List.of("login_totp", "totp", "otpauth", "totp secret"),
            "type", List.of("type"));

    private EntryCsv() {}

    /** @throws IllegalArgumentException si no parece un CSV de contraseñas */
    public static Imported read(String csv) {
        List<List<String>> rows = Csv.parse(csv);
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }
        Map<String, Integer> col = new HashMap<>();
        List<String> header = rows.get(0);
        for (Map.Entry<String, List<String>> field : SYNONYMS.entrySet()) {
            for (String name : field.getValue()) {
                int idx = indexOf(header, name);
                if (idx >= 0) {
                    col.put(field.getKey(), idx);
                    break;
                }
            }
        }
        if (!col.containsKey("password") || !(col.containsKey("title") || col.containsKey("url") || col.containsKey("username"))) {
            throw new IllegalArgumentException("Formato no reconocido: faltan columnas de nombre/usuario/contraseña");
        }

        List<PasswordEntry> entries = new ArrayList<>();
        int skipped = 0;
        for (List<String> row : rows.subList(1, rows.size())) {
            String type = cell(row, col.get("type"));
            if (!type.isEmpty() && !type.equalsIgnoreCase("login")) { // notas seguras, tarjetas, identidades…
                skipped++;
                continue;
            }
            String url = firstLine(cell(row, col.get("url")));
            String title = cell(row, col.get("title"));
            if (title.isEmpty()) title = hostOf(url);
            if (title.isEmpty()) title = cell(row, col.get("username"));
            if (title.isEmpty()) {
                skipped++;
                continue;
            }
            PasswordEntry e = new PasswordEntry(title, cell(row, col.get("username")), cell(row, col.get("password")),
                    url, cell(row, col.get("notes")));
            String category = cell(row, col.get("category"));
            if (!category.isEmpty()) e.setCategory(category);
            e.setFavorite(cell(row, col.get("favorite")).equals("1") || cell(row, col.get("favorite")).equalsIgnoreCase("true"));
            String totp = cell(row, col.get("totp"));
            if (!totp.isEmpty()) e.setTotpSecret(totp);
            entries.add(e);
        }
        return new Imported(entries, skipped);
    }

    public static String write(List<PasswordEntry> entries) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(EXPORT_HEADER);
        for (PasswordEntry e : entries) {
            rows.add(List.of(nz(e.getCategory()), e.isFavorite() ? "1" : "", "login", nz(e.getTitle()), nz(e.getNotes()), "",
                    "0", nz(e.getUrl()), nz(e.getUsername()), nz(e.getPassword()), nz(e.getTotpSecret())));
        }
        return Csv.write(rows);
    }

    /** Escribe el CSV (contraseñas en claro) con permisos solo para el propietario, sustituyendo el archivo si existe. */
    public static void writeFile(java.nio.file.Path path, List<PasswordEntry> entries) throws java.io.IOException {
        java.nio.file.Files.deleteIfExists(path);
        if (java.nio.file.FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            java.nio.file.Files.createFile(path, java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
        }
        java.nio.file.Files.writeString(path, write(entries), java.nio.charset.StandardCharsets.UTF_8);
    }

    private static int indexOf(List<String> header, String name) {
        for (int i = 0; i < header.size(); i++) {
            if (header.get(i).trim().toLowerCase(Locale.ROOT).equals(name)) return i;
        }
        return -1;
    }

    private static String cell(List<String> row, Integer idx) {
        return idx == null || idx >= row.size() ? "" : row.get(idx).trim();
    }

    private static String firstLine(String s) {
        int nl = s.indexOf('\n');
        return (nl >= 0 ? s.substring(0, nl) : s).trim();
    }

    private static String hostOf(String url) {
        if (url.isEmpty()) return "";
        try {
            String host = java.net.URI.create(url.contains("://") ? url : "https://" + url).getHost();
            return host == null ? "" : host.replaceFirst("^www\\.", "");
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
