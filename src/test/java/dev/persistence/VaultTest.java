package dev.persistence;

import dev.security.VaultCrypto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class VaultTest {
    private static final char[] PWD = "correct horse battery staple".toCharArray();

    @TempDir
    Path dir;

    private Path newVault() throws VaultException {
        Path file = dir.resolve("test.ckey");
        Vault.create(file, PWD).close();
        return file;
    }

    private static byte[] bytes(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @Test
    void createdVaultOpensWithEmptyList() throws Exception {
        Path file = newVault();
        try (Vault v = Vault.open(file, PWD)) {
            assertEquals("[]", new String(v.data(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void createRefusesToOverwrite() throws Exception {
        Path file = newVault();
        assertThrows(VaultException.class, () -> Vault.create(file, PWD));
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        Path file = newVault();
        assertThrows(VaultException.InvalidPassword.class, () -> Vault.open(file, "nope".toCharArray()));
    }

    @Test
    void saveRoundTrips() throws Exception {
        Path file = newVault();
        try (Vault v = Vault.open(file, PWD)) {
            v.save(bytes("[{\"id\":1}]"));
        }
        try (Vault v = Vault.open(file, PWD)) {
            assertEquals("[{\"id\":1}]", new String(v.data(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void eachSaveUsesAFreshNonce() throws Exception {
        Path file = newVault();
        try (Vault v = Vault.open(file, PWD)) {
            v.save(bytes("same"));
            byte[] first = Files.readAllBytes(file);
            v.save(bytes("same"));
            byte[] second = Files.readAllBytes(file);
            assertFalse(Arrays.equals(first, second));
        }
    }

    @Test
    void saveKeepsBackupOfPreviousVersionAndNoTmpLeftover() throws Exception {
        Path file = newVault();
        byte[] before = Files.readAllBytes(file);
        try (Vault v = Vault.open(file, PWD)) {
            v.save(bytes("[1]"));
        }
        assertArrayEquals(before, Files.readAllBytes(dir.resolve("test.ckey.bak")));
        assertFalse(Files.exists(dir.resolve("test.ckey.tmp")));
    }

    @Test
    void tamperedCiphertextIsRejected() throws Exception {
        Path file = newVault();
        byte[] content = Files.readAllBytes(file);
        content[content.length - 1] ^= 1;
        Files.write(file, content);
        assertThrows(VaultException.InvalidPassword.class, () -> Vault.open(file, PWD));
    }

    @Test
    void tamperedHeaderIsRejected() throws Exception {
        // La sal está en la cabecera: al cambiarla la clave derivada ya difiere, pero además el AAD no coincide.
        Path file = newVault();
        byte[] content = Files.readAllBytes(file);
        content[20] ^= 1; // dentro de la sal
        Files.write(file, content);
        assertThrows(VaultException.InvalidPassword.class, () -> Vault.open(file, PWD));
    }

    @Test
    void headerKdfParamsAreAuthenticated() throws Exception {
        // Bajar las iteraciones en la cabecera (rebaja de seguridad) debe detectarse aunque la clave coincida.
        Path file = newVault();
        byte[] content = Files.readAllBytes(file);
        // iteraciones: offset 4+1+1+4 = 10..13 (int big endian), valor por defecto 3
        assertEquals(3, content[13]);
        content[13] = 2;
        Files.write(file, content);
        assertThrows(VaultException.InvalidPassword.class, () -> Vault.open(file, PWD));
    }

    @Test
    void badMagicVersionAndTruncationAreFormatErrors() throws Exception {
        Path file = newVault();
        byte[] content = Files.readAllBytes(file);

        byte[] badMagic = content.clone();
        badMagic[0] = 'X';
        Files.write(file, badMagic);
        assertThrows(VaultException.BadFormat.class, () -> Vault.open(file, PWD));

        byte[] badVersion = content.clone();
        badVersion[4] = 1;
        Files.write(file, badVersion);
        assertThrows(VaultException.BadFormat.class, () -> Vault.open(file, PWD));

        Files.write(file, Arrays.copyOf(content, 10));
        assertThrows(VaultException.BadFormat.class, () -> Vault.open(file, PWD));
    }

    @Test
    void absurdKdfParamsInHeaderAreRejectedBeforeDerivingKey() throws Exception {
        Path file = newVault();
        byte[] content = Files.readAllBytes(file);
        content[6] = 0x7F; // memoria ~2 GiB
        Files.write(file, content);
        assertThrows(VaultException.BadFormat.class, () -> Vault.open(file, PWD));
    }

    @Test
    void changePasswordRotatesSaltAndInvalidatesOldPassword() throws Exception {
        Path file = newVault();
        byte[] oldContent = Files.readAllBytes(file);
        char[] newPwd = "another long passphrase".toCharArray();
        try (Vault v = Vault.open(file, PWD)) {
            v.changePassword(newPwd, bytes("[2]"));
        }
        byte[] newContent = Files.readAllBytes(file);
        int saltStart = 15, saltEnd = saltStart + VaultCrypto.SALT_LENGTH;
        assertFalse(Arrays.equals(Arrays.copyOfRange(oldContent, saltStart, saltEnd),
                Arrays.copyOfRange(newContent, saltStart, saltEnd)));
        assertThrows(VaultException.InvalidPassword.class, () -> Vault.open(file, PWD));
        try (Vault v = Vault.open(file, newPwd)) {
            assertEquals("[2]", new String(v.data(), StandardCharsets.UTF_8));
        }
    }

    @Test
    void saveAfterCloseFails() throws Exception {
        Path file = newVault();
        Vault v = Vault.open(file, PWD);
        v.close();
        assertThrows(IllegalStateException.class, () -> v.save(bytes("x")));
    }

    @Test
    void openDoesNotModifyThePasswordArray() throws Exception {
        Path file = newVault();
        char[] pwd = PWD.clone();
        Vault.open(file, pwd).close();
        assertArrayEquals(PWD, pwd);
    }

    @Test
    void missingFileIsReportedAsVaultException() {
        assertThrows(VaultException.class, () -> Vault.open(dir.resolve("nope.ckey"), PWD));
    }
}
