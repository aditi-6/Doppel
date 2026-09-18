package com.doppel.scanner;

import com.doppel.exception.InvalidDirectoryException;
import com.doppel.model.FileMetadata;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DirectoryScanner {

    public List<FileMetadata> scan(String directoryPath)
            throws InvalidDirectoryException {

        Path root;

        try {
            root = Paths.get(directoryPath);
        } catch (Exception e) {
            throw new InvalidDirectoryException(
                    "Invalid directory path: " + directoryPath, e);
        }

        if (!Files.exists(root)) {
            throw new InvalidDirectoryException(
                    "Directory does not exist: " + directoryPath);
        }

        if (!Files.isDirectory(root)) {
            throw new InvalidDirectoryException(
                    "Path is not a directory: " + directoryPath);
        }

        List<Path> discoveredFiles = new ArrayList<>();

        /*
         * First, recursively discover all regular files.
         */
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(
                        Path file,
                        BasicFileAttributes attrs) {

                    if (attrs.isRegularFile()) {
                        discoveredFiles.add(file);
                    }

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(
                        Path file,
                        IOException exc) {

                    System.out.println(
                            "Skipping inaccessible file: " + file);

                    return FileVisitResult.CONTINUE;
                }
            });

        } catch (IOException e) {
            throw new InvalidDirectoryException(
                    "Unable to scan directory: " + directoryPath, e);
        }

        /*
         * Process discovered files concurrently.
         */
        ConcurrentLinkedQueue<FileMetadata> files =
                new ConcurrentLinkedQueue<>();

        int threadCount = Math.max(
                2,
                Runtime.getRuntime().availableProcessors());

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        List<Future<?>> futures = new ArrayList<>();

        for (Path file : discoveredFiles) {

            Future<?> future = executor.submit(() -> {

                try {
                    BasicFileAttributes attrs =
                            Files.readAttributes(
                                    file,
                                    BasicFileAttributes.class);

                    FileMetadata metadata = new FileMetadata(
                            file.toAbsolutePath()
                                    .normalize()
                                    .toString(),
                            attrs.size(),
                            attrs.lastModifiedTime()
                                    .toMillis());

                    files.add(metadata);

                } catch (IOException e) {
                    System.out.println(
                            "Skipping inaccessible file: " + file);
                }
            });

            futures.add(future);
        }

        /*
         * Wait for all worker tasks to finish.
         */
        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                System.out.println(
                        "A file-processing task could not be completed.");
            }
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(
                    1,
                    TimeUnit.MINUTES)) {

                executor.shutdownNow();
            }

        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        List<FileMetadata> result =
                new ArrayList<>(files);

        result.sort(
                Comparator.comparing(
                        FileMetadata::getFilePath));

        return result;
    }
}
