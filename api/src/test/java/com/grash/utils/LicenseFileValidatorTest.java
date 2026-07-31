package com.grash.utils;

import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Hex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LicenseFileValidatorTest {

    @TempDir
    Path tempDir;

    private static final String LICENSE_KEY = "test-license-key-123";
    private static final String PLAINTEXT = "{\"license\":\"valid\",\"expiry\":\"2027-01-01\"}";
    private static final String SUPPORTED_ALGORITHM = "aes-256-gcm+ed25519";
    private static final String HEADER = "-----BEGIN LICENSE FILE-----";
    private static final String FOOTER = "-----END LICENSE FILE-----";

    private static final class EncryptResult {
        final String enc;
        final String sig;
        final String publicKeyHex;

        EncryptResult(String enc, String sig, String publicKeyHex) {
            this.enc = enc;
            this.sig = sig;
            this.publicKeyHex = publicKeyHex;
        }
    }

    private static EncryptResult encryptAndSign() throws Exception {
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(new SecureRandom());
        Ed25519PublicKeyParameters publicKey = privateKey.generatePublicKey();

        byte[] key =
                MessageDigest.getInstance("SHA-256").digest(LicenseFileValidatorTest.LICENSE_KEY.getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] plaintextBytes = LicenseFileValidatorTest.PLAINTEXT.getBytes(StandardCharsets.UTF_8);

        GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());
        cipher.init(true, new AEADParameters(new KeyParameter(key), 128, iv, null));
        byte[] out = new byte[cipher.getOutputSize(plaintextBytes.length)];
        int len = cipher.processBytes(plaintextBytes, 0, plaintextBytes.length, out, 0);
        cipher.doFinal(out, len);

        byte[] ciphertext = Arrays.copyOfRange(out, 0, out.length - 16);
        byte[] tag = Arrays.copyOfRange(out, out.length - 16, out.length);
        String enc = Base64.getEncoder().encodeToString(ciphertext) + "."
                + Base64.getEncoder().encodeToString(iv) + "."
                + Base64.getEncoder().encodeToString(tag);

        return new EncryptResult(enc, signEnc(enc, privateKey, publicKey), Hex.toHexString(publicKey.getEncoded()));
    }

    private static String signEnc(String enc, Ed25519PrivateKeyParameters privateKey,
                                  Ed25519PublicKeyParameters publicKey) {
        String signingData = "license/" + enc;
        byte[] signingDataBytes = signingData.getBytes(StandardCharsets.UTF_8);
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        signer.update(signingDataBytes, 0, signingDataBytes.length);
        return Base64.getEncoder().encodeToString(signer.generateSignature());
    }

    private static String buildLicenseFile(String enc, String sig, String algorithm) {
        String payload = "{\"enc\":\"" + enc + "\",\"sig\":\"" + sig + "\",\"alg\":\"" + algorithm + "\"}";
        String encoded = Base64.getEncoder().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return HEADER + "\n" + encoded + "\n" + FOOTER;
    }

    private Path writeLicenseFile(String content) throws IOException {
        Path file = tempDir.resolve("license.lic");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void validLicenseFile_returnsPlaintext() throws Exception {
        EncryptResult valid = encryptAndSign();
        Path file = writeLicenseFile(buildLicenseFile(valid.enc, valid.sig, SUPPORTED_ALGORITHM));

        assertEquals(PLAINTEXT, LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), LICENSE_KEY, valid.publicKeyHex));
    }

    @Test
    void missingFile_returnsNull() {
        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                tempDir.resolve("does-not-exist.lic").toString(), LICENSE_KEY, "00"));
    }

    @Test
    void invalidBase64Payload_returnsNull() throws IOException {
        Path file = writeLicenseFile(HEADER + "\n!!!not-base64!!!\n" + FOOTER);

        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), LICENSE_KEY, "00"));
    }

    @Test
    void invalidJsonPayload_returnsNull() throws IOException {
        String encoded = Base64.getEncoder().encodeToString("this is not json".getBytes(StandardCharsets.UTF_8));
        Path file = writeLicenseFile(HEADER + "\n" + encoded + "\n" + FOOTER);

        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), LICENSE_KEY, "00"));
    }

    @Test
    void unsupportedAlgorithm_returnsNull() throws Exception {
        EncryptResult valid = encryptAndSign();
        Path file = writeLicenseFile(buildLicenseFile(valid.enc, valid.sig, "aes-128-gcm"));

        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), LICENSE_KEY, valid.publicKeyHex));
    }

    @Test
    void tamperedEncryptedData_returnsNull() throws Exception {
        EncryptResult valid = encryptAndSign();
        char last = valid.enc.charAt(valid.enc.length() - 1);
        String tampered = valid.enc.substring(0, valid.enc.length() - 1) + (last == 'A' ? 'B' : 'A');
        Path file = writeLicenseFile(buildLicenseFile(tampered, valid.sig, SUPPORTED_ALGORITHM));

        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), LICENSE_KEY, valid.publicKeyHex));
    }

    @Test
    void invalidSignatureEncoding_returnsNull() throws Exception {
        EncryptResult valid = encryptAndSign();
        Path file = writeLicenseFile(buildLicenseFile(valid.enc, "!!!", SUPPORTED_ALGORITHM));

        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), LICENSE_KEY, valid.publicKeyHex));
    }

    @Test
    void wrongLicenseKey_returnsNull() throws Exception {
        EncryptResult valid = encryptAndSign();
        Path file = writeLicenseFile(buildLicenseFile(valid.enc, valid.sig, SUPPORTED_ALGORITHM));

        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), "wrong-key", valid.publicKeyHex));
    }

    @Test
    void invalidEncryptedDataFormat_returnsNull() throws Exception {
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(new SecureRandom());
        Ed25519PublicKeyParameters publicKey = privateKey.generatePublicKey();
        String enc = "ciphertext-without-separator";
        String sig = signEnc(enc, privateKey, publicKey);
        Path file = writeLicenseFile(buildLicenseFile(enc, sig, SUPPORTED_ALGORITHM));

        assertNull(LicenseFileValidator.validateAndDecryptLicenseFile(
                file.toString(), LICENSE_KEY, Hex.toHexString(publicKey.getEncoded())));
    }

    @Test
    void licenseFileExists_existingFile_returnsTrue() throws IOException {
        Path file = writeLicenseFile("content");
        assertTrue(LicenseFileValidator.licenseFileExists(file.toString()));
    }

    @Test
    void licenseFileExists_missingFile_returnsFalse() {
        assertFalse(LicenseFileValidator.licenseFileExists(
                tempDir.resolve("nope.lic").toString()));
    }
}
