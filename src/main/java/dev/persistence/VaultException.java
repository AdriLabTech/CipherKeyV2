package dev.persistence;

/** Error al abrir o guardar una bóveda. */
public class VaultException extends Exception {
    public VaultException(String message) {
        super(message);
    }

    public VaultException(String message, Throwable cause) {
        super(message, cause);
    }

    /** La clave maestra es incorrecta, o el contenido de la bóveda fue manipulado (GCM no distingue ambos casos). */
    public static class InvalidPassword extends VaultException {
        public InvalidPassword(Throwable cause) {
            super("Contraseña incorrecta o bóveda manipulada", cause);
        }
    }

    /** El fichero no es una bóveda CipherKey válida o su versión no está soportada. */
    public static class BadFormat extends VaultException {
        public BadFormat(String message) {
            super(message);
        }
    }
}
