package com.spoiledmilk.spritebaker;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

final class Hashing {
    private Hashing() {
    }

    static String sha256(Path path) throws IOException {
        MessageDigest digest=digest();
        try (InputStream input = Files.newInputStream(path)) {
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
        }
        return hex(digest.digest());
    }

    static String sha256(String value) {
        MessageDigest digest=digest();digest.update(value.getBytes(StandardCharsets.UTF_8));return hex(digest.digest());
    }

    private static MessageDigest digest(){
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }
    private static String hex(byte[] bytes){
        StringBuilder hex = new StringBuilder(64);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
