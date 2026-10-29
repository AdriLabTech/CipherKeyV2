package dev.persistence;

import dev.model.PasswordEntry;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EntryCsvTest {
    @Test
    void importsBitwardenSkippingNonLoginItems() {
        String csv = "folder,favorite,type,name,notes,fields,reprompt,login_uri,login_username,login_password,login_totp\n"
                + "Dev,1,login,GitHub,\"nota, con coma\",,0,https://github.com,adrian,s3cret,JBSWY3DPEHPK3PXP\n"
                + ",0,note,Mi nota,texto,,0,,,,\n"
                + ",0,login,,,,0,\"https://www.example.com/login\nhttps://otra.com\",bob,pw,\n";
        EntryCsv.Imported r = EntryCsv.read(csv);

        assertEquals(1, r.skipped());
        assertEquals(2, r.entries().size());
        PasswordEntry gh = r.entries().get(0);
        assertEquals("GitHub", gh.getTitle());
        assertEquals("Dev", gh.getCategory());
        assertTrue(gh.isFavorite());
        assertEquals("nota, con coma", gh.getNotes());
        assertEquals("JBSWY3DPEHPK3PXP", gh.getTotpSecret());
        PasswordEntry noTitle = r.entries().get(1);
        assertEquals("example.com", noTitle.getTitle());
        assertEquals("https://www.example.com/login", noTitle.getUrl());
    }

    @Test
    void importsChromeFormat() {
        EntryCsv.Imported r = EntryCsv.read("name,url,username,password,note\nGoogle,https://accounts.google.com,me,pw1,\n");
        assertEquals(1, r.entries().size());
        assertEquals("me", r.entries().get(0).getUsername());
        assertEquals("pw1", r.entries().get(0).getPassword());
    }

    @Test
    void importsKeePassFormat() {
        EntryCsv.Imported r = EntryCsv.read(
                "\"Group\",\"Title\",\"Username\",\"Password\",\"URL\",\"Notes\"\n\"Root/Web\",\"Foro\",\"yo\",\"clave\",\"http://foro.es\",\"ok\"\n");
        PasswordEntry e = r.entries().get(0);
        assertEquals("Foro", e.getTitle());
        assertEquals("Root/Web", e.getCategory());
    }

    @Test
    void rejectsUnrecognisedAndEmptyFiles() {
        assertThrows(IllegalArgumentException.class, () -> EntryCsv.read("a,b,c\n1,2,3\n"));
        assertThrows(IllegalArgumentException.class, () -> EntryCsv.read(""));
    }

    @Test
    void exportThenImportRoundTrips() {
        PasswordEntry e = new PasswordEntry("Mi \"sitio\"", "user,x", "p@ss\nword", "https://a.com", "notas");
        e.setCategory("Cat");
        e.setFavorite(true);
        e.setTotpSecret("JBSWY3DPEHPK3PXP");

        PasswordEntry back = EntryCsv.read(EntryCsv.write(List.of(e))).entries().get(0);

        assertEquals(e.getTitle(), back.getTitle());
        assertEquals(e.getUsername(), back.getUsername());
        assertEquals(e.getPassword(), back.getPassword());
        assertEquals("Cat", back.getCategory());
        assertTrue(back.isFavorite());
        assertEquals("JBSWY3DPEHPK3PXP", back.getTotpSecret());
    }

    @org.junit.jupiter.api.Test
    void writeFileIsOwnerOnlyAndReplacesExistingFile(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
        Path out = dir.resolve("export.csv");
        Files.writeString(out, "viejo");
        EntryCsv.writeFile(out, List.of(new PasswordEntry("A", "u", "p", "", "")));

        assertTrue(Files.readString(out).contains("login_password"));
        assertFalse(Files.readString(out).contains("viejo"));
        if (Files.getFileStore(out).supportsFileAttributeView("posix")) {
            assertEquals("rw-------", PosixFilePermissions.toString(Files.getPosixFilePermissions(out)));
        }
    }
}
