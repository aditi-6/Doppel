package com.doppel.similarity;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class JaccardSimilarity implements SimilarityStrategy {

    @Override
    public double compare(String textA, String textB) {

        Set<String> setA = tokenize(textA);
        Set<String> setB = tokenize(textB);

        if (setA.isEmpty() && setB.isEmpty()) {
            return 100.0;
        }

        if (setA.isEmpty() || setB.isEmpty()) {
            return 0.0;
        }

        Set<String> intersection =
                new HashSet<>(setA);

        intersection.retainAll(setB);

        Set<String> union =
                new HashSet<>(setA);

        union.addAll(setB);

        return (intersection.size() * 100.0)
                / union.size();
    }

    private Set<String> tokenize(String text) {

        if (text == null || text.isBlank()) {
            return new HashSet<>();
        }

        String normalized =
                text.toLowerCase(Locale.ROOT)
                        .replaceAll("[^\\p{L}\\p{Nd}]+", " ")
                        .trim();

        if (normalized.isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(normalized.split("\\s+"))
                .filter(token -> !token.isBlank())
                .collect(Collectors.toSet());
    }
}
