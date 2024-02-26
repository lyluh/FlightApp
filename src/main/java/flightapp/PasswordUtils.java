package flightapp;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Properties;

import java.util.Random;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/**
 * A collection of utility methods to help with managing passwords
 */
public class PasswordUtils {
  /**
   * Generates a cryptographically-secure salted password.
   */
  public static byte[] saltAndHashPassword(String password) {
    byte[] salt = generateSalt();
    byte[] saltedHash = hashWithSalt(password, salt);
    byte[] result = new byte[salt.length + saltedHash.length];

    for (int i = 0; i < salt.length; i++) {
      result[i] = salt[i];
    }

    for (int i = salt.length; i < result.length; i++) {
      result[i] = saltedHash[i - salt.length];
    }
    // System.arraycopy(salt, 0, saltedHash, 0, salt.length);
    // System.arraycopy(saltedHash, 0, result, salt.length, saltedHash.length);
    return result;
  }

  /**
   * Verifies whether the plaintext password can be hashed to provided salted hashed password.
   */
  public static boolean plaintextMatchesSaltedHash(String plaintext, byte[] saltedHashed) {
    // extract the salt from the byte array (ie, undo the logic you implemented in https://edstem.org/us/courses/488/discussion/
    // saltAndHashPassword), then use it to check whether the user-provided plaintext
    // password matches the password hash.
    // byte[] salt = Arrays.copyOfRange(saltedHashed, 0, SALT_LENGTH_BYTES);
    byte[] salt = new byte[SALT_LENGTH_BYTES];
    for (int i = 0; i < salt.length; i++) {
      salt[i] = saltedHashed[i];
    }

    byte[] hash = new byte[saltedHashed.length - salt.length];
    for (int i = salt.length; i < saltedHashed.length; i++) {
      hash[i - salt.length] = saltedHashed[i];
    }

    return Arrays.equals(hashWithSalt(plaintext, salt), hash);
  }
  
  // Password hashing parameter constants.
  private static final int HASH_STRENGTH = 65536;
  private static final int KEY_LENGTH_BYTES = 128;
  private static final int SALT_LENGTH_BYTES = 16;

  /**
   * Generate a small bit of randomness to serve as a password "salt"
   */
  static byte[] generateSalt() {
    byte[] salt = new byte[SALT_LENGTH_BYTES];
    Random r = new Random();
    r.nextBytes(salt);
    return salt;
  }

  /**
   * Uses the provided salt to generate a cryptographically-secure hash of the provided password.
   * The resultant byte array will be KEY_LENGTH_BYTES bytes long.
   */
  static byte[] hashWithSalt(String password, byte[] salt)
    throws IllegalStateException {
    // Specify the hash parameters, including the salt
    KeySpec spec = new PBEKeySpec(password.toCharArray(), salt,
                                  HASH_STRENGTH, KEY_LENGTH_BYTES * 8 /* length in bits */);

    // Hash the whole thing
    SecretKeyFactory factory = null;
    byte[] hash = null; 
    try {
      factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
      hash = factory.generateSecret(spec).getEncoded();
      return hash;
    } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
      throw new IllegalStateException();
    }
  }

}
