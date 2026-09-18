package com.doppel.detector;

import com.doppel.model.DuplicateGroup;
import com.doppel.model.FileMetadata;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class DuplicateDetector {

    private final HashCalculator hashCalculator;

    public DuplicateDetector() {
        this.hashCalculator = new HashCalculator();
    }

    public List<DuplicateGroup> findDuplicates(
            List<FileMetadata> files) {

        /*
         * First filter by file size.
         * Files with different sizes cannot be exact duplicates.
         */
        Map<Long, List<FileMetadata>> sizeGroups =
                new ConcurrentHashMap<>();

        for (FileMetadata file : files) {
            sizeGroups
                    .computeIfAbsent(
                            file.getFileSize(),
                            key -> new CopyOnWriteArrayList<>()
                    )
                    .add(file);
        }

        /*
         * Only files whose size occurs more than once
         * need SHA-256 calculation.
         */
        List<FileMetadata> candidates = new ArrayList<>();

        for (List<FileMetadata> group : sizeGroups.values()) {
            if (group.size() > 1) {
                candidates.addAll(group);
            }
        }

        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }

        /*
         * Thread-safe map.
         *
         * Key = file size + SHA-256.
         */
        Map<String, List<FileMetadata>> hashGroups =
                new ConcurrentHashMap<>();

        int threadCount = Math.max(
                2,
                Runtime.getRuntime().availableProcessors()
        );

        ExecutorService executor =
                Executors.newFixedThreadPool(threadCount);

        List<Future<?>> futures = new ArrayList<>();

        for (FileMetadata file : candidates) {

            Future<?> future = executor.submit(() -> {

                try {
                    Path path = Paths.get(file.getFilePath());

                    String hash =
                            hashCalculator.calculateSHA256(path);

                    file.setSha256Hash(hash);

                    String key =
                            file.getFileSize() + ":" + hash;

                    hashGroups
                            .computeIfAbsent(
                                    key,
                                    k -> new CopyOnWriteArrayList<>()
                            )
                            .add(file);

                } catch (Exception e) {

                    System.out.println(
                            "Unable to hash file: "
                                    + file.getFilePath()
                    );
                }
            });

            futures.add(future);
        }

        for (Future<?> future : futures) {
            try {
                future.get();
            } catch (Exception e) {
                System.out.println(
                        "A hashing task could not be completed."
                );
            }
        }

        executor.shutdown();

        try {
            if (!executor.awaitTermination(1, TimeUnit.MINUTES)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        List<DuplicateGroup> duplicateGroups =
                new ArrayList<>();

        for (Map.Entry<String, List<FileMetadata>> entry
                : hashGroups.entrySet()) {

            List<FileMetadata> groupFiles = entry.getValue();

            if (groupFiles.size() > 1) {

                String hash = groupFiles.get(0).getSha256Hash();

                long fileSize = groupFiles.get(0).getFileSize();

                duplicateGroups.add(
                        new DuplicateGroup(
                                hash,
                                fileSize,
                                groupFiles
                        )
                );
            }
        }

        duplicateGroups.sort(
                Comparator.comparing(
                        DuplicateGroup::getSha256Hash
                )
        );

        return duplicateGroups;
    }
}
