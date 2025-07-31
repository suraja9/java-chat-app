package util;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public class AESUtil {
    private static final String SECRET_KEY_ALGO = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_SIZE = 256;

    // You can generate this once and store securely; or transmit once per session
    private static final String SALT = "a9v5n38s"; // just for testing

    public static SecretKeySpec getKeyFromPassword(String password) throws Exception {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), SALT.getBytes(), ITERATIONS, KEY_SIZE);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(SECRET_KEY_ALGO);
        byte[] key = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(key, "AES");
    }

    public static String encrypt(String data, SecretKeySpec key) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        byte[] iv = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        cipher.init(Cipher.ENCRYPT_MODE, key, ivSpec);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        byte[] encryptedWithIv = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, iv.length);
        System.arraycopy(encrypted, 0, encryptedWithIv, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }

    public static String decrypt(String encryptedData, SecretKeySpec key) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(encryptedData);
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[decoded.length - 16];
        System.arraycopy(decoded, 0, iv, 0, 16);
        System.arraycopy(decoded, 16, encrypted, 0, encrypted.length);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);

        byte[] original = cipher.doFinal(encrypted);
        return new String(original);
    }
}
