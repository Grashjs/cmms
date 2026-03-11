package com.grash.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.AESEngine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Hex;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Validates and decrypts license files using AES-256-GCM encryption and Ed25519 signatures.
 * License files are expected to be in the format produced by Keygen.sh with offline validation support.
 */
@Slf4j
public class LicenseFileValidator {

    private static final String LICENSE_FILE_HEADER = "-----BEGIN LICENSE FILE-----";
    private static final String LICENSE_FILE_FOOTER = "-----END LICENSE FILE-----";
    private static final String SUPPORTED_ALGORITHM = "aes-256-gcm+ed25519";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validates and decrypts a license file, returning the plaintext license data.
     *
     * @param licenseFilePath Path to the license.lic file
     * @param licenseKey      The license key used for decryption
     * @param publicKey       The hex-encoded public key for signature verification
     * @return Decrypted license data as a JSON string, or null if validation fails
     */
    public static String validateAndDecryptLicenseFile(String licenseFilePath, String licenseKey, String publicKey) {
        try {
            // Read license file content
            String licenseFileContent = Files.readString(Paths.get(licenseFilePath));

            // Parse signed license file (removing cert header, newlines and footer)
            String encodedPayload = licenseFileContent.replaceAll(
                    "(^" + LICENSE_FILE_HEADER + "\\n|\\n|" + LICENSE_FILE_FOOTER + "\\n?$)",
                    ""
            );

            byte[] payloadBytes = Base64.getDecoder().decode(encodedPayload);
            String payload = new String(payloadBytes);
            String encryptedData;
            String encodedSignature;
            String algorithm;

            try {
                JsonNode attrs = objectMapper.readTree(payload);
                encryptedData = attrs.get("enc").asText();
                encodedSignature = attrs.get("sig").asText();
                algorithm = attrs.get("alg").asText();
            } catch (IOException e) {
                log.error("Failed to parse license file: {}", e.getMessage());
                return null;
            }

            // Verify license file algorithm
            if (!algorithm.equals(SUPPORTED_ALGORITHM)) {
                log.error("Unsupported algorithm: {}. Expected: {}", algorithm, SUPPORTED_ALGORITHM);
                return null;
            }

            // Decode base64 signature and signing data to byte arrays
            byte[] signatureBytes = Base64.getDecoder().decode(encodedSignature);
            String signingData = String.format("license/%s", encryptedData);
            byte[] signingDataBytes = signingData.getBytes();

            // Convert hex-encoded public key to a byte array
            byte[] publicKeyBytes = Hex.decode(publicKey);

            // Set up Ed25519 verifier
            Ed25519PublicKeyParameters verifierParams = new Ed25519PublicKeyParameters(publicKeyBytes, 0);
            Ed25519Signer verifier = new Ed25519Signer();

            verifier.init(false, verifierParams);
            verifier.update(signingDataBytes, 0, signingDataBytes.length);

            // Verify the signature
            boolean ok = verifier.verifySignature(signatureBytes);
            if (!ok) {
                log.error("License file signature is invalid");
                return null;
            }

            log.info("License file signature is valid");

            // Decrypt the license file
            String plaintext = decryptLicenseData(encryptedData, licenseKey);
            if (plaintext == null) {
                return null;
            }

            log.info("License file was successfully decrypted");
            return plaintext;

        } catch (IOException e) {
            log.error("Failed to read license file: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.error("Invalid license file format: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Decrypts the encrypted license data using AES-256-GCM.
     *
     * @param encryptedData The encrypted data string (ciphertext.iv.tag format)
     * @param licenseKey    The license key used to derive the decryption key
     * @return Decrypted plaintext or null if decryption fails
     */
    private static String decryptLicenseData(String encryptedData, String licenseKey) {
        try {
            // Parse the encrypted data (format: ciphertext.iv.tag)
            String[] parts = encryptedData.split("\\.", 3);
            if (parts.length != 3) {
                log.error("Invalid encrypted data format");
                return null;
            }

            String encodedCiphertext = parts[0];
            String encodedIv = parts[1];
            String encodedTag = parts[2];

            // Decode ciphertext, IV and tag to byte arrays
            byte[] ciphertext = Base64.getDecoder().decode(encodedCiphertext);
            byte[] iv = Base64.getDecoder().decode(encodedIv);
            byte[] tag = Base64.getDecoder().decode(encodedTag);

            // Hash license key with SHA-256 to obtain encryption key
            byte[] key;
            try {
                byte[] licenseKeyBytes = licenseKey.getBytes();
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                sha256.update(licenseKeyBytes);
                key = sha256.digest();
            } catch (NoSuchAlgorithmException e) {
                log.error("Failed to hash license key: {}", e.getMessage());
                return null;
            }

            // Set up AES-256-GCM
            AEADParameters cipherParams = new AEADParameters(new KeyParameter(key), 128, iv, null);
            GCMBlockCipher cipher = new GCMBlockCipher(new AESEngine());

            cipher.init(false, cipherParams);

            // Concat ciphertext and authentication tag to produce cipher input
            byte[] input = new byte[ciphertext.length + tag.length];
            System.arraycopy(ciphertext, 0, input, 0, ciphertext.length);
            System.arraycopy(tag, 0, input, ciphertext.length, tag.length);

            // Decrypt the ciphertext
            byte[] output = new byte[cipher.getOutputSize(input.length)];
            int len = cipher.processBytes(input, 0, input.length, output, 0);

            // Validate authentication tag
            cipher.doFinal(output, len);

            return new String(output);

        } catch (IllegalArgumentException | IllegalStateException | DataLengthException | InvalidCipherTextException e) {
            log.error("Failed to decrypt license file: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Checks if a license file exists at the specified path.
     */
    public static boolean licenseFileExists(String licenseFilePath) {
        Path path = Paths.get(licenseFilePath);
        return Files.exists(path) && Files.isReadable(path);
    }
}
