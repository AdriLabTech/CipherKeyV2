package dev.persistence;

import dev.security.VaultCrypto;
import dev.security.VaultCrypto.KdfParams;

import javax.crypto.AEADBadTagException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.GeneralSecurityException;
import java.util.Arrays;

/**
 * Bóveda abierta: fichero .ckey + clave derivada en memoria.
 *
 * <p>Formato v2 (big endian):
 * <pre>
 * "CKEY"(4) | versión(1)=2 | kdf(1)=1 Argon2id | memoriaKiB(4) | iteraciones(4) | paralelismo(1) | sal(32)
 * | nonce(12) | texto cifrado + tag GCM(16)
 * </pre>
 * Todo lo anterior al nonce es la cabecera y se autentica como AAD: alterarla invalida el descifrado.
 *
 * <p>La KDF solo se ejecuta al abrir o al cambiar la clave maestra; guardar solo cifra.
 */
public final class Vault implements AutoCloseable {
    private static final byte[] MAGIC = "CKEY".getBytes(StandardCharsets.US_ASCII);
    private static final byte VERSION = 2;
    private static final byte KDF_ARGON2ID = 1;
    private static final int HEADER_LENGTH = MAGIC.length + 1 + 1 + 4 + 4 + 1 + VaultCrypto.SALT_LENGTH;
    private static final int MIN_FILE_LENGTH = HEADER_LENGTH + VaultCrypto.NONCE_LENGTH + 16;

    private final Path file;
    private byte[] header;
    private byte[] key;
    private final byte[] data;

    private Vault(Path file, byte[] header, byte[] key, byte[] data) {
        this.file = file;
        this.header = header;
        this.key = key;
        this.data = data;
    }

    /** Crea un fichero nuevo con contenido vacío ({@code []}). Falla si ya existe. */
    public static Vault create(Path file, char[] password) throws VaultException {
        if (Files.exists(file)) {
            throw new VaultException("El archivo ya existe: " + file);
        }
        byte[] header = buildHeader(KdfParams.DEFAULT, VaultCrypto.randomBytes(VaultCrypto.SALT_LENGTH));
        byte[] key = VaultCrypto.deriveKey(password, saltOf(header), KdfParams.DEFAULT);
        byte[] empty = "[]".getBytes(StandardCharsets.UTF_8);
        Vault vault = new Vault(file, header, key, empty);
        vault.write(empty, false);
        return vault;
    }

    public static Vault open(Path file, char[] password) throws VaultException {
        byte[] content;
        try {
            content = Files.readAllBytes(file);
        } catch (IOException e) {
            throw new VaultException("No se pudo leer la bóveda: " + e.getMessage(), e);
        }
        if (content.length < MIN_FILE_LENGTH) {
            throw new VaultException.BadFormat("Archivo demasiado corto para ser una bóveda");
        }
        ByteBuffer buf = ByteBuffer.wrap(content);
        byte[] magic = new byte[MAGIC.length];
        buf.get(magic);
        if (!Arrays.equals(magic, MAGIC)) {
            throw new VaultException.BadFormat("No es una bóveda CipherKey");
        }
        byte version = buf.get();
        if (version != VERSION) {
            throw new VaultException.BadFormat("Versión de bóveda no soportada: " + version);
        }
        byte kdf = buf.get();
        if (kdf != KDF_ARGON2ID) {
            throw new VaultException.BadFormat("KDF no soportada: " + kdf);
        }
        KdfParams params = new KdfParams(buf.getInt(), buf.getInt(), buf.get() & 0xFF);
        if (!params.isSane()) {
            throw new VaultException.BadFormat("Parámetros de derivación fuera de rango");
        }

        byte[] header = Arrays.copyOfRange(content, 0, HEADER_LENGTH);
        byte[] key = VaultCrypto.deriveKey(password, saltOf(header), params);
        try {
            byte[] plaintext = VaultCrypto.decrypt(key, header, Arrays.copyOfRange(content, HEADER_LENGTH, content.length));
            return new Vault(file, header, key, plaintext);
        } catch (AEADBadTagException e) {
            Arrays.fill(key, (byte) 0);
            throw new VaultException.InvalidPassword(e);
        } catch (GeneralSecurityException e) {
            Arrays.fill(key, (byte) 0);
            throw new VaultException("Error al descifrar la bóveda", e);
        }
    }

    /** Contenido descifrado tal y como estaba al abrir la bóveda. */
    public byte[] data() {
        return data.clone();
    }

    /** Cifra y guarda de forma atómica, dejando la versión anterior como {@code <fichero>.bak}. */
    public void save(byte[] plaintext) throws VaultException {
        ensureOpen();
        write(plaintext, true);
    }

    /** Cambia la clave maestra con una sal nueva y reescribe la bóveda. */
    public void changePassword(char[] newPassword, byte[] plaintext) throws VaultException {
        ensureOpen();
        byte[] newHeader = buildHeader(KdfParams.DEFAULT, VaultCrypto.randomBytes(VaultCrypto.SALT_LENGTH));
        byte[] newKey = VaultCrypto.deriveKey(newPassword, saltOf(newHeader), KdfParams.DEFAULT);
        byte[] oldHeader = header;
        byte[] oldKey = key;
        header = newHeader;
        key = newKey;
        try {
            write(plaintext, true);
        } catch (VaultException e) {
            header = oldHeader;
            key = oldKey;
            Arrays.fill(newKey, (byte) 0);
            throw e;
        }
        Arrays.fill(oldKey, (byte) 0);
    }

    @Override
    public void close() {
        if (key != null) {
            Arrays.fill(key, (byte) 0);
            key = null;
        }
        Arrays.fill(data, (byte) 0);
    }

    private void ensureOpen() {
        if (key == null) {
            throw new IllegalStateException("La bóveda está cerrada");
        }
    }

    private void write(byte[] plaintext, boolean keepBackup) throws VaultException {
        byte[] body;
        try {
            body = VaultCrypto.encrypt(key, header, plaintext);
        } catch (GeneralSecurityException e) {
            throw new VaultException("Error al cifrar la bóveda", e);
        }
        byte[] content = ByteBuffer.allocate(header.length + body.length).put(header).put(body).array();

        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.deleteIfExists(tmp);
            createPrivateFile(tmp);
            try (FileChannel ch = FileChannel.open(tmp, StandardOpenOption.WRITE)) {
                ch.write(ByteBuffer.wrap(content));
                ch.force(true);
            }
            if (keepBackup && Files.exists(file)) {
                Files.copy(file, file.resolveSibling(file.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            try {
                Files.deleteIfExists(tmp);
            } catch (IOException ignored) {
                // el error original es el relevante
            }
            throw new VaultException("No se pudo guardar la bóveda: " + e.getMessage(), e);
        }
    }

    private static void createPrivateFile(Path path) throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.createFile(path, PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rw-------")));
        } else {
            Files.createFile(path);
        }
    }

    private static byte[] buildHeader(KdfParams params, byte[] salt) {
        return ByteBuffer.allocate(HEADER_LENGTH)
                .put(MAGIC).put(VERSION).put(KDF_ARGON2ID)
                .putInt(params.memoryKiB()).putInt(params.iterations()).put((byte) params.parallelism())
                .put(salt)
                .array();
    }

    private static byte[] saltOf(byte[] header) {
        return Arrays.copyOfRange(header, HEADER_LENGTH - VaultCrypto.SALT_LENGTH, HEADER_LENGTH);
    }
}
