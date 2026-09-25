package com.example.androidfeasibility;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Android Keystore-backed provider credential storage. */
public final class AndroidCredentialStore implements ProviderCredentialStore {
    private static final String PREFS = "mochi_provider_credentials";
    private static final String KEY_ALIAS = "mochi_provider_credentials_key_v1";
    private static final String VALUE_PREFIX = "v1:";
    private final SharedPreferences preferences;

    public AndroidCredentialStore(Context context) {
        if (context == null) throw new IllegalArgumentException("context is null");
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override public synchronized void put(String providerId, String secret) {
        requireProvider(providerId);
        if (secret == null || secret.isEmpty()) throw new IllegalArgumentException("secret is empty");
        try {
            byte[] iv = new byte[12];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            byte[] ciphertext = cipher.doFinal(secret.getBytes(StandardCharsets.UTF_8));
            String value = VALUE_PREFIX + encode(iv) + ":" + encode(ciphertext);
            preferences.edit().putString(providerId, value).apply();
        } catch (Exception error) {
            throw new IllegalStateException("credential encryption failed", error);
        }
    }

    @Override public synchronized String lookupForTransport(String providerId) {
        requireProvider(providerId);
        String value = preferences.getString(providerId, "");
        if (value.isEmpty()) return "";
        try {
            String[] parts = value.split(":", -1);
            if (parts.length != 3 || !VALUE_PREFIX.substring(0, VALUE_PREFIX.length() - 1).equals(parts[0])) {
                return "";
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, decode(parts[1])));
            return new String(cipher.doFinal(decode(parts[2])), StandardCharsets.UTF_8);
        } catch (Exception error) {
            return "";
        }
    }

    @Override public synchronized boolean has(String providerId) {
        return !lookupForTransport(providerId).isEmpty();
    }

    @Override public synchronized String masked(String providerId) {
        String secret = lookupForTransport(providerId);
        if (secret.isEmpty()) return "";
        String suffix = secret.length() <= 4 ? secret : secret.substring(secret.length() - 4);
        return "••••••••" + suffix;
    }

    @Override public synchronized void remove(String providerId) {
        requireProvider(providerId);
        preferences.edit().remove(providerId).apply();
    }

    private SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(KEY_ALIAS)) return ((KeyStore.SecretKeyEntry)
                store.getEntry(KEY_ALIAS, null)).getSecretKey();
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private static void requireProvider(String providerId) {
        if (providerId == null || providerId.isEmpty()) throw new IllegalArgumentException("providerId is empty");
    }

    private static String encode(byte[] bytes) {
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }

    private static byte[] decode(String value) {
        return Base64.decode(value, Base64.NO_WRAP);
    }
}
