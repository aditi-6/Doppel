package com.doppel.report;

import com.doppel.model.DuplicateGroup;
import com.doppel.model.FileMetadata;
import com.doppel.model.ScanResult;
import com.doppel.model.SimilarityResult;
import com.doppel.util.FileUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;

public class ReportGenerator {

    public void printConsoleReport(
            ScanResult result) {

        System.out.println();
        System.out.println("========================================");
        System.out.println("           DOPPEL SCAN REPORT");
        System.out.println("========================================");

        System.out.println(
                "Directory: "
                        + result.getScannedDirectory()
        );

        System.out.println(
                "Total files scanned: "
                        + result.getTotalFilesScanned()
        );

        System.out.println(
                "Exact duplicate groups: "
                        + result.getDuplicateGroups().size()
        );

        System.out.println(
                "Similar text pairs: "
                        + result.getSimilarityResults().size()
        );

        System.out.println();
        System.out.println("---------- EXACT DUPLICATES ----------");

        List<DuplicateGroup> groups =
                result.getDuplicateGroups();

        if (groups.isEmpty()) {

            System.out.println(
                    "No exact duplicate groups found."
            );

        } else {

            int groupNumber = 1;

            for (DuplicateGroup group : groups) {

                System.out.println();
                System.out.println(
                        "Group " + groupNumber++
                );

                System.out.println(
                        "SHA-256: "
                                + group.getSha256Hash()
                );

                System.out.println(
                        "File size: "
                                + group.getFileSize()
                                + " bytes"
                );

                System.out.println(
                        "Files: "
                                + group.getDuplicateCount()
                );

                for (FileMetadata file :
                        group.getFiles()) {

                    System.out.println(
                            "  - "
                                    + file.getFilePath()
                    );
                }
            }
        }

        System.out.println();
        System.out.println("---------- SIMILAR TEXT FILES ----------");

        List<SimilarityResult> similarities =
                result.getSimilarityResults();

        if (similarities.isEmpty()) {

            System.out.println(
                    "No similar text file pairs found."
            );

        } else {

            for (SimilarityResult similarity :
                    similarities) {

                System.out.printf(
                        Locale.ROOT,
                        "%.2f%% | %s <-> %s%n",
                        similarity.getSimilarityPercentage(),
                        similarity.getFileAPath(),
                        similarity.getFileBPath()
                );
            }
        }

        System.out.println();
        System.out.println("========================================");
    }

    public void exportCsv(
            ScanResult result,
            String outputFile)
            throws IOException {

        Path path =
                Paths.get(outputFile);

        try (BufferedWriter writer =
                     Files.newBufferedWriter(path)) {

            writer.write(
                    "Type,File A,File B,Hash,File Size,Similarity"
            );

            writer.newLine();

            for (DuplicateGroup group :
                    result.getDuplicateGroups()) {

                List<FileMetadata> files =
                        group.getFiles();

                for (FileMetadata file : files) {

                    writer.write(
                            FileUtils.escapeCsv("EXACT_DUPLICATE")
                    );
                    writer.write(",");

                    writer.write(
                            FileUtils.escapeCsv(
                                    file.getFilePath()
                            )
                    );
                    writer.write(",");

                    writer.write(
                            FileUtils.escapeCsv("")
                    );
                    writer.write(",");

                    writer.write(
                            FileUtils.escapeCsv(
                                    group.getSha256Hash()
                            )
                    );
                    writer.write(",");

                    writer.write(
                            String.valueOf(
                                    group.getFileSize()
                            )
                    );
                    writer.write(",");

                    writer.write("");
                    writer.newLine();
                }
            }

            for (SimilarityResult similarity :
                    result.getSimilarityResults()) {

                writer.write(
                        FileUtils.escapeCsv(
                                "SIMILAR_TEXT"
                        )
                );
                writer.write(",");

                writer.write(
                        FileUtils.escapeCsv(
                                similarity.getFileAPath()
                        )
                );
                writer.write(",");

                writer.write(
                        FileUtils.escapeCsv(
                                similarity.getFileBPath()
                        )
                );
                writer.write(",");

                writer.write("");
                writer.write(",");

                writer.write("");
                writer.write(",");

                writer.write(
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                similarity
                                        .getSimilarityPercentage()
                        )
                );

                writer.newLine();
            }
        }
    }
}
