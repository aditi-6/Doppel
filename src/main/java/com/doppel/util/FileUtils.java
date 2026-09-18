package com.doppel.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public final class FileUtils {

    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "txt",
            "text",
            "java",
            "py",
            "c",
            "cpp",
            "h",
            "html",
            "htm",
            "css",
            "js",
            "json",
            "xml",
            "csv",
            "md"
    );

    private FileUtils() {
    }

    public static boolean isTextFile(String filePath) {

        String fileName =
                Paths.get(filePath)
                        .getFileName()
                        .toString();

        int dotIndex = fileName.lastIndexOf('.');

        if (dotIndex == -1 ||
                dotIndex == fileName.length() - 1) {
            return false;
        }

        String extension =
                fileName.substring(dotIndex + 1)
                        .toLowerCase();

        return TEXT_EXTENSIONS.contains(extension);
    }

    public static String readTextFile(String filePath)
            throws IOException {

        Path path = Paths.get(filePath);

        return Files.readString(
                path,
                StandardCharsets.UTF_8
        );
    }

    public static String escapeCsv(String value) {

        if (value == null) {
            return "";
        }

        String escaped =
                value.replace("\"", "\"\"");

        return "\"" + escaped + "\"";
    }
}
