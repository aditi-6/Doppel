package com.doppel.model;

import java.util.Locale;

public class SimilarityResult {

    private final String fileAPath;
    private final String fileBPath;
    private final double similarityPercentage;

    public SimilarityResult(
            String fileAPath,
            String fileBPath,
            double similarityPercentage) {

        this.fileAPath = fileAPath;
        this.fileBPath = fileBPath;
        this.similarityPercentage = similarityPercentage;
    }

    public String getFileAPath() {
        return fileAPath;
    }

    public String getFileBPath() {
        return fileBPath;
    }

    public double getSimilarityPercentage() {
        return similarityPercentage;
    }

    @Override
    public String toString() {
        return String.format(
                Locale.ROOT,
                "SimilarityResult{%s <-> %s : %.2f%%}",
                fileAPath,
                fileBPath,
                similarityPercentage
        );
    }
}
