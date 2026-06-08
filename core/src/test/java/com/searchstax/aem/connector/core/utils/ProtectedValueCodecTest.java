package com.searchstax.aem.connector.core.utils;

import com.adobe.granite.crypto.CryptoException;
import com.adobe.granite.crypto.CryptoSupport;
import com.searchstax.aem.connector.core.testcontext.TestReflection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProtectedValueCodecTest {

    private static final String PROTECTED =
            "{d2acbf3c6f50f9215d9e864e6727631fc5d230cbb135718de8c9fd4cb42e174bf2223267f9f2cbc703ad1dcb5d742da591f68bacb2e39ee90d8a2eaab7f39084}";

    @Mock
    private CryptoSupport cryptoSupport;

    @Test
    void decryptsProtectedValue() throws CryptoException {
        final ProtectedValueCodec target = newCodec(cryptoSupport);

        when(cryptoSupport.isProtected(PROTECTED)).thenReturn(true);
        when(cryptoSupport.unprotect(PROTECTED)).thenReturn("plain-update-token");

        assertEquals("plain-update-token", target.unprotectIfNeeded(PROTECTED));
    }

    @Test
    void returnsPlainValueWhenNotProtected() throws CryptoException {
        final ProtectedValueCodec target = newCodec(cryptoSupport);

        when(cryptoSupport.isProtected("plain-token")).thenReturn(false);

        assertEquals("plain-token", target.unprotectIfNeeded("plain-token"));
        assertFalse(target.looksEncrypted("plain-token"));
    }

    @Test
    void detectsEncryptedBlobPattern() {
        final ProtectedValueCodec target = newCodec(null);

        assertTrue(target.looksEncrypted(PROTECTED));
    }

    @Test
    void returnsEmptyForNullOrBlankValues() {
        final ProtectedValueCodec target = newCodec(cryptoSupport);

        assertEquals("", target.unprotectIfNeeded(null));
        assertEquals("", target.unprotectIfNeeded("  "));
        assertFalse(target.looksEncrypted(null));
    }

    @Test
    void returnsTrimmedValueWhenCryptoSupportUnavailable() {
        final ProtectedValueCodec target = newCodec(null);

        assertEquals("plain-token", target.unprotectIfNeeded(" plain-token "));
    }

    @Test
    void attemptsUnprotectForProtectedBlobWhenNotMarkedProtected() throws CryptoException {
        final ProtectedValueCodec target = newCodec(cryptoSupport);

        when(cryptoSupport.isProtected(PROTECTED)).thenReturn(false);
        when(cryptoSupport.unprotect(PROTECTED)).thenReturn("decrypted");

        assertEquals("decrypted", target.unprotectIfNeeded(PROTECTED));
    }

    @Test
    void returnsTrimmedValueWhenDecryptionFails() throws CryptoException {
        final ProtectedValueCodec target = newCodec(cryptoSupport);

        when(cryptoSupport.isProtected(PROTECTED)).thenReturn(true);
        when(cryptoSupport.unprotect(PROTECTED)).thenThrow(new CryptoException("failed"));

        assertEquals(PROTECTED, target.unprotectIfNeeded(PROTECTED));
    }

    @Test
    void detectsEncryptedValueViaCryptoSupport() throws CryptoException {
        final ProtectedValueCodec target = newCodec(cryptoSupport);

        when(cryptoSupport.isProtected("protected-value")).thenReturn(true);

        assertTrue(target.looksEncrypted("protected-value"));
    }

    private static ProtectedValueCodec newCodec(final CryptoSupport cryptoSupport) {
        final ProtectedValueCodec target = new ProtectedValueCodec();
        TestReflection.inject(target, "cryptoSupport", cryptoSupport);
        return target;
    }
}
