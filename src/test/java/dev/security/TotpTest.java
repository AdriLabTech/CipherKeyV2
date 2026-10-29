package dev.security;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class TotpTest {
    // Vectores de prueba del RFC 6238 (apéndice B), 8 dígitos.
    private static final String SHA1_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ"; // "12345678901234567890"
    private static final String SHA256_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZA"; // 32 bytes
    private static final String SHA512_SECRET =
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQGEZDGNA"; // 64 bytes

    private static Instant t(long s) {
        return Instant.ofEpochSecond(s);
    }

    private static String uri(String secret, String algo) {
        return "otpauth://totp/x?secret=" + secret + "&algorithm=" + algo + "&digits=8";
    }

    @Test
    void rfc6238Sha1Vectors() {
        Totp totp = Totp.parse(uri(SHA1_SECRET, "SHA1"));
        assertEquals("94287082", totp.code(t(59)));
        assertEquals("07081804", totp.code(t(1111111109L)));
        assertEquals("89005924", totp.code(t(1234567890L)));
        assertEquals("65353130", totp.code(t(20000000000L)));
    }

    @Test
    void rfc6238Sha256AndSha512Vectors() {
        assertEquals("46119246", Totp.parse(uri(SHA256_SECRET, "SHA256")).code(t(59)));
        assertEquals("90693936", Totp.parse(uri(SHA512_SECRET, "SHA512")).code(t(59)));
    }

    @Test
    void plainBase32DefaultsToSixDigitsSha1ThirtySeconds() {
        Totp totp = Totp.parse("gezd gnbv-gy3t qojq gezd gnbv gy3t qojq");
        assertEquals("287082", totp.code(t(59)));
        assertEquals(30, totp.period());
    }

    @Test
    void secondsRemainingCountsDownWithinThePeriod() {
        Totp totp = Totp.parse(SHA1_SECRET);
        assertEquals(30, totp.secondsRemaining(t(60)));
        assertEquals(1, totp.secondsRemaining(t(89)));
    }

    @Test
    void otpauthWithLabelAndUrlEncodedParams() {
        Totp totp = Totp.parse("otpauth://totp/GitHub:adrian%40dev.io?issuer=GitHub&secret=" + SHA1_SECRET + "&period=60");
        assertEquals(60, totp.period());
    }

    @Test
    void rejectsInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> Totp.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Totp.parse("no es base32 1089!"));
        assertThrows(IllegalArgumentException.class, () -> Totp.parse("otpauth://hotp/x?secret=ABCD"));
        assertThrows(IllegalArgumentException.class, () -> Totp.parse("otpauth://totp/x?issuer=a"));
        assertThrows(IllegalArgumentException.class, () -> Totp.parse("otpauth://totp/x?secret=ABCD&digits=99"));
        assertThrows(IllegalArgumentException.class, () -> Totp.parse("otpauth://totp/x?secret=ABCD&algorithm=MD5"));
    }
}
