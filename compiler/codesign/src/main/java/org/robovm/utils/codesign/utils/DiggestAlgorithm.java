package org.robovm.utils.codesign.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public enum DiggestAlgorithm {
    SignatureNoHash(null, 0, 0),            // null value
    HashSHA1("SHA-1", 1, 20),               // SHA-1
    HashSHA256("SHA-256", 2, 32),          // SHA-256
    HashSHA256Truncated("SHA-256", 3, 20), // SHA-256 truncated to first 20 bytes
    HashSHA384("SHA-384", 4, 48);          // SHA-384

    public final String algorithm;
    public final int hashType; // as in CodeDirectory blob
    public final int hashSize;
    DiggestAlgorithm(String algorithm, int v, int hashSize) {
        this.algorithm = algorithm;
        this.hashType = v;
        this.hashSize = hashSize;
    }

    public static DiggestAlgorithm fromCodeDirValue(int v) {
        if (v >= 0 && v < DiggestAlgorithm.values().length)
            return DiggestAlgorithm.values()[v];
        return null;
    }

    public byte[] hash( byte[] a) {
        return hash(a, 0, a.length);
    }

    public byte[] hash(byte[] a, int offset, int size) {
        try {
            MessageDigest hasher = MessageDigest.getInstance(algorithm);
            hasher.update(a, offset, size);
            byte[] bytes = hasher.digest();
            return (bytes.length > this.hashSize) ? Arrays.copyOf(bytes, this.hashSize) : bytes;
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] hash(File path) {
        final int HASH_BLOCKSIZE = 65536;
        try {
            MessageDigest hasher = MessageDigest.getInstance(algorithm);
            InputStream is = new FileInputStream(path);
            byte[] buf = new byte[HASH_BLOCKSIZE];
            int bytesRead;
            while ((bytesRead = is.read(buf)) != -1)
                hasher.update(buf, 0, bytesRead);
            byte[] bytes = hasher.digest();
            return (bytes.length > this.hashSize) ? Arrays.copyOf(bytes, this.hashSize) : bytes;
        } catch (NoSuchAlgorithmException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
