package com.doppel.util;

import com.doppel.exception.InvalidDirectoryException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class InputValidator {

    private InputValidator() {
    }

    public static Path validateDirectory(String directory)
            throws InvalidDirectoryException {

        if (directory == null ||
                directory.trim().isEmpty()) {

            throw new InvalidDirectoryException(
                    "Directory path cannot be empty."
            );
        }

        Path path;

        try {
            path = Paths.get(directory.trim());
        } catch (Exception e) {
            throw new InvalidDirectoryException(
                    "Invalid directory path.",
                    e
            );
        }

        if (!Files.exists(path)) {
            throw new InvalidDirectoryException(
                    "Directory does not exist: " + directory
            );
        }

        if (!Files.isDirectory(path)) {
            throw new InvalidDirectoryException(
                    "Path is not a directory: " + directory
            );
        }

        return path.toAbsolutePath().normalize();
    }

    public static double validateThreshold(String input) {

        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Threshold cannot be empty."
            );
        }

        double threshold;

        try {
            threshold = Double.parseDouble(
                    input.trim()
            );
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Threshold must be a number between 0 and 100."
            );
        }

        if (threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException(
                    "Threshold must be between 0 and 100."
            );
        }

        return threshold;
    }
}
