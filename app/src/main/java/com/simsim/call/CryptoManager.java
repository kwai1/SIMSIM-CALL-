package com.simsim.call;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;
import java.security.SecureRandom;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Local end-to-end message encryption.
 * The private RSA key is generated and kept in Android Keystore; it is never uploaded.
 * Each message gets a fresh AES-256-GCM key, wrapped separately for both chat members.
 */
public final class CryptoManager {
    private static final String KEY_ALIAS = "simsim_e2ee_rsa_v1";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final OAEPParameterSpec OAEP = new OAEPParameterSpec(
            "SHA-256", "MGF1", MGF1ParameterSpec.SHA1, PSource.PSpecified.DEFAULT);

    private CryptoManager() {}

    private static KeyStore keyStore() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
        ks.load(null);
        return ks;
    }

    private static void ensureKeyPair() throws Exception {
        KeyStore ks = keyStore();
        if (ks.containsAlias(KEY_ALIAS)) return;

        KeyPairGenerator generator = KeyPairGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(2048)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .build();
        generator.initialize(spec);
        generator.generateKeyPair();
    }

    public static String getPublicKeyBase64() throws Exception {
        ensureKeyPair();
        PublicKey key = keyStore().getCertificate(KEY_ALIAS).getPublicKey();
        return Base64.encodeToString(key.getEncoded(), Base64.NO_WRAP);
    }

    private static PublicKey decodePublicKey(String encoded) throws Exception {
        byte[] bytes = Base64.decode(encoded, Base64.NO_WRAP);
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(bytes));
    }

    public static Map<String, Object> encryptForChat(
            String plainText,
            String myUid,
            String myPublicKey,
            String otherUid,
            String otherPublicKey) throws Exception {

        if (plainText == null || myUid == null || otherUid == null
                || myPublicKey == null || otherPublicKey == null) {
            throw new IllegalArgumentException("Missing encryption identity or public key");
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
        keyGenerator.init(256);
        SecretKey messageKey = keyGenerator.generateKey();

        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.ENCRYPT_MODE, messageKey, new GCMParameterSpec(128, iv));
        byte[] cipherText = aes.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        Map<String, String> wrappedKeys = new LinkedHashMap<>();
        wrappedKeys.put(myUid, wrapKey(messageKey.getEncoded(), decodePublicKey(myPublicKey)));
        wrappedKeys.put(otherUid, wrapKey(messageKey.getEncoded(), decodePublicKey(otherPublicKey)));

        Map<String, Object> result = new HashMap<>();
        result.put("ciphertext", Base64.encodeToString(cipherText, Base64.NO_WRAP));
        result.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP));
        result.put("wrappedKeys", wrappedKeys);
        result.put("encryptionVersion", 1);
        return result;
    }

    public static String fingerprint(String publicKeyBase64) throws Exception {
        byte[] encoded = Base64.decode(publicKeyBase64, Base64.NO_WRAP);
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(encoded);
        StringBuilder hex = new StringBuilder();
        for (byte value : digest) hex.append(String.format("%02X", value));
        StringBuilder grouped = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 4) {
            if (i > 0) grouped.append(' ');
            grouped.append(hex, i, Math.min(i + 4, hex.length()));
        }
        return grouped.toString();
    }

    public static String createCallSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return Base64.encodeToString(secret, Base64.NO_WRAP);
    }

    private static String wrapKey(byte[] rawKey, PublicKey publicKey) throws Exception {
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.ENCRYPT_MODE, publicKey, OAEP);
        return Base64.encodeToString(rsa.doFinal(rawKey), Base64.NO_WRAP);
    }

    public static String decryptMessage(
            String ciphertextBase64, String ivBase64, Map<String, Object> wrappedKeys,
            String myUid) throws Exception {
        if (ciphertextBase64 == null || ivBase64 == null || wrappedKeys == null || myUid == null) {
            throw new IllegalArgumentException("Encrypted message is incomplete");
        }
        Object wrapped = wrappedKeys.get(myUid);
        if (!(wrapped instanceof String)) {
            throw new IllegalStateException("This device has no decryption key for this message");
        }

        ensureKeyPair();
        KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry)
                keyStore().getEntry(KEY_ALIAS, null);
        Cipher rsa = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
        rsa.init(Cipher.DECRYPT_MODE, entry.getPrivateKey(), OAEP);
        byte[] rawAesKey = rsa.doFinal(Base64.decode((String) wrapped, Base64.NO_WRAP));

        javax.crypto.SecretKeySpec aesKey = new javax.crypto.spec.SecretKeySpec(rawAesKey, "AES");
        Cipher aes = Cipher.getInstance("AES/GCM/NoPadding");
        aes.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(
                128, Base64.decode(ivBase64, Base64.NO_WRAP)));
        byte[] plain = aes.doFinal(Base64.decode(ciphertextBase64, Base64.NO_WRAP));
        return new String(plain, StandardCharsets.UTF_8);
    }
}
