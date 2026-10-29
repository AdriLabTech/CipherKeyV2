package dev.security;

import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Primitivas criptográficas de la bóveda: Argon2id para derivar la clave y AES-256-GCM para cifrar.
 * No modifica los arrays que recibe; quien los aporta es responsable de borrarlos.
 */
public final class VaultCrypto {
    public static final int KEY_LENGTH = 32;
    public static final int SALT_LENGTH = 32;
    public static final int NONCE_LENGTH = 12;
    private static final int GCM_TAG_BITS = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    private VaultCrypto() {}

    /** Parámetros de Argon2id. Se guardan en la cabecera del fichero para poder subirlos en el futuro. */
    public record KdfParams(int memoryKiB, int iterations, int parallelism) {
        public static final KdfParams DEFAULT = new KdfParams(64 * 1024, 3, 1);

        // Topes para que un fichero manipulado no pueda forzar un consumo de memoria/CPU absurdo.
        private static final int MAX_MEMORY_KIB = 1024 * 1024;
        private static final int MAX_ITERATIONS = 20;
        private static final int MAX_PARALLELISM = 16;

        public boolean isSane() {
            return memoryKiB >= 8 * parallelism && memoryKiB <= MAX_MEMORY_KIB
                    && iterations >= 1 && iterations <= MAX_ITERATIONS
                    && parallelism >= 1 && parallelism <= MAX_PARALLELISM;
        }
    }

    public static byte[] randomBytes(int length) {
        byte[] bytes = new byte[length];
        RANDOM.nextBytes(bytes);
        return bytes;
    }

    /** Deriva una clave de 32 bytes. El llamador debe poner a cero el resultado cuando termine. */
    public static byte[] deriveKey(char[] password, byte[] salt, KdfParams params) {
        byte[] pwdBytes = toUtf8(password);
        try {
            Argon2Parameters argon = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                    .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                    .withSalt(salt)
                    .withMemoryAsKB(params.memoryKiB())
                    .withIterations(params.iterations())
                    .withParallelism(params.parallelism())
                    .build();
            Argon2BytesGenerator generator = new Argon2BytesGenerator();
            generator.init(argon);
            byte[] key = new byte[KEY_LENGTH];
            generator.generateBytes(pwdBytes, key);
            return key;
        } finally {
            Arrays.fill(pwdBytes, (byte) 0);
        }
    }

    /** Cifra con AES-256-GCM. {@code aad} queda autenticado pero no cifrado. Devuelve nonce + texto cifrado. */
    public static byte[] encrypt(byte[] key, byte[] aad, byte[] plaintext) throws GeneralSecurityException {
        byte[] nonce = randomBytes(NONCE_LENGTH);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        byte[] ciphertext = cipher.doFinal(plaintext);
        return ByteBuffer.allocate(NONCE_LENGTH + ciphertext.length).put(nonce).put(ciphertext).array();
    }

    /**
     * Descifra lo producido por {@link #encrypt}.
     *
     * @throws AEADBadTagException si la clave es incorrecta o los datos/cabecera fueron alterados
     */
    public static byte[] decrypt(byte[] key, byte[] aad, byte[] nonceAndCiphertext) throws GeneralSecurityException {
        if (nonceAndCiphertext.length < NONCE_LENGTH + GCM_TAG_BITS / 8) {
            throw new AEADBadTagException("Datos cifrados demasiado cortos");
        }
        byte[] nonce = Arrays.copyOfRange(nonceAndCiphertext, 0, NONCE_LENGTH);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
        cipher.updateAAD(aad);
        return cipher.doFinal(nonceAndCiphertext, NONCE_LENGTH, nonceAndCiphertext.length - NONCE_LENGTH);
    }

    private static byte[] toUtf8(char[] chars) {
        ByteBuffer buf = StandardCharsets.UTF_8.encode(CharBuffer.wrap(chars));
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);
        Arrays.fill(buf.array(), (byte) 0);
        return bytes;
    }
}
