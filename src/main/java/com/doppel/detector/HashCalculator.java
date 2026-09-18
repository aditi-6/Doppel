package com.doppel.detector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCalculator {

    private static final int BUFFER_SIZE = 8192;

    public String calculateSHA256(Path file) throws IOException {

        MessageDigest digest;

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.",
                    e
            );
        }

        try (InputStream inputStream = Files.newInputStream(file)) {

            byte[] buffer = new byte[BUFFER_SIZE];

            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        byte[] hashBytes = digest.digest();

        StringBuilder hash = new StringBuilder();

        for (byte b : hashBytes) {
            hash.append(String.format("%02x", b));
        }

        return hash.toString();
    }
}
