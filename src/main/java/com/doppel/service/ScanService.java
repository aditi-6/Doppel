package com.doppel.service;

import com.doppel.database.ScanRepository;
import com.doppel.exception.DatabaseOperationException;
import com.doppel.exception.InvalidDirectoryException;
import com.doppel.model.DuplicateGroup;
import com.doppel.model.FileMetadata;
import com.doppel.model.ScanResult;
import com.doppel.model.SimilarityResult;
import com.doppel.detector.DuplicateDetector;
import com.doppel.scanner.DirectoryScanner;
import com.doppel.similarity.SimilarityStrategy;
import com.doppel.util.FileUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ScanService {

    private final DirectoryScanner directoryScanner;
    private final DuplicateDetector duplicateDetector;
    private final SimilarityStrategy similarityStrategy;
    private final ScanRepository scanRepository;

    public ScanService() {
        directoryScanner = new DirectoryScanner();
        duplicateDetector = new DuplicateDetector();
        similarityStrategy =
                new com.doppel.similarity.JaccardSimilarity();
        scanRepository = new ScanRepository();
    }

    public List<FileMetadata> scanDirectory(
            String directoryPath)
            throws InvalidDirectoryException {

        return directoryScanner.scan(directoryPath);
    }

    public List<DuplicateGroup> findExactDuplicates(
            List<FileMetadata> files) {

        return duplicateDetector.findDuplicates(files);
    }

    public List<SimilarityResult> findSimilarTextFiles(
            List<FileMetadata> files,
            double threshold) {

        List<FileMetadata> textFiles =
                new ArrayList<>();

        for (FileMetadata file : files) {

            if (FileUtils.isTextFile(
                    file.getFilePath())) {

                textFiles.add(file);
            }
        }

        List<SimilarityResult> results =
                new ArrayList<>();

        for (int i = 0; i < textFiles.size(); i++) {

            FileMetadata fileA = textFiles.get(i);

            String textA;

            try {
                textA = FileUtils.readTextFile(
                        fileA.getFilePath()
                );
            } catch (IOException e) {
                System.out.println(
                        "Unable to read: "
                                + fileA.getFilePath()
                );
                continue;
            }

            for (int j = i + 1;
                 j < textFiles.size();
                 j++) {

                FileMetadata fileB =
                        textFiles.get(j);

                String textB;

                try {
                    textB = FileUtils.readTextFile(
                            fileB.getFilePath()
                    );
                } catch (IOException e) {
                    System.out.println(
                            "Unable to read: "
                                    + fileB.getFilePath()
                    );
                    continue;
                }

                double similarity =
                        similarityStrategy.compare(
                                textA,
                                textB
                        );

                if (similarity >= threshold) {

                    results.add(
                            new SimilarityResult(
                                    fileA.getFilePath(),
                                    fileB.getFilePath(),
                                    similarity
                            )
                    );
                }
            }
        }

        return results;
    }

    public ScanResult createScanResult(
            String directoryPath,
            List<FileMetadata> files,
            List<DuplicateGroup> duplicateGroups,
            List<SimilarityResult> similarityResults) {

        return new ScanResult(
                directoryPath,
                files.size(),
                duplicateGroups,
                similarityResults
        );
    }

    public long saveScan(ScanResult scanResult,
                         List<FileMetadata> files)
            throws DatabaseOperationException {

        return scanRepository.saveScan(
                scanResult.getScannedDirectory(),
                files,
                scanResult.getDuplicateGroups(),
                scanResult.getSimilarityResults()
        );
    }

    public List<String> getScanHistory()
            throws DatabaseOperationException {

        return scanRepository.getScanHistory();
    }
}
