package com.doppel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScanResult {

    private final String scannedDirectory;
    private final int totalFilesScanned;
    private final List<DuplicateGroup> duplicateGroups;
    private final List<SimilarityResult> similarityResults;

    public ScanResult(
            String scannedDirectory,
            int totalFilesScanned,
            List<DuplicateGroup> duplicateGroups,
            List<SimilarityResult> similarityResults) {

        this.scannedDirectory = scannedDirectory;
        this.totalFilesScanned = totalFilesScanned;

        this.duplicateGroups = Collections.unmodifiableList(
                new ArrayList<>(duplicateGroups)
        );

        this.similarityResults = Collections.unmodifiableList(
                new ArrayList<>(similarityResults)
        );
    }

    public String getScannedDirectory() {
        return scannedDirectory;
    }

    public int getTotalFilesScanned() {
        return totalFilesScanned;
    }

    public List<DuplicateGroup> getDuplicateGroups() {
        return duplicateGroups;
    }

    public List<SimilarityResult> getSimilarityResults() {
        return similarityResults;
    }

    @Override
    public String toString() {
        return "ScanResult{" +
                "dir='" + scannedDirectory + '\'' +
                ", filesScanned=" + totalFilesScanned +
                ", duplicateGroups=" + duplicateGroups.size() +
                ", similarPairs=" + similarityResults.size() +
                '}';
    }
}
