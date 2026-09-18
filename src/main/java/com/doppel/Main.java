package com.doppel;

import com.doppel.exception.DatabaseOperationException;
import com.doppel.exception.InvalidDirectoryException;
import com.doppel.model.DuplicateGroup;
import com.doppel.model.FileMetadata;
import com.doppel.model.ScanResult;
import com.doppel.model.SimilarityResult;
import com.doppel.report.ReportGenerator;
import com.doppel.service.ScanService;
import com.doppel.util.InputValidator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner scanner =
            new Scanner(System.in);

    private static final ScanService scanService =
            new ScanService();

    private static final ReportGenerator reportGenerator =
            new ReportGenerator();

    private static List<FileMetadata> currentFiles =
            new ArrayList<>();

    private static List<DuplicateGroup> currentDuplicates =
            new ArrayList<>();

    private static List<SimilarityResult> currentSimilarities =
            new ArrayList<>();

    private static String currentDirectory = null;

    private static ScanResult currentScan = null;

    public static void main(String[] args) {

        System.out.println();
        System.out.println("========================================");
        System.out.println("              DOPPEL");
        System.out.println("   Multithreaded Duplicate Detection");
        System.out.println("========================================");

        boolean running = true;

        while (running) {

            printMenu();

            String choice =
                    scanner.nextLine().trim();

            switch (choice) {

                case "1":
                    scanDirectory();
                    break;

                case "2":
                    findExactDuplicates();
                    break;

                case "3":
                    findSimilarTextFiles();
                    break;

                case "4":
                    viewScanHistory();
                    break;

                case "5":
                    generateReport();
                    break;

                case "6":
                    running = false;
                    System.out.println(
                            "Exiting Doppel."
                    );
                    break;

                default:
                    System.out.println(
                            "Invalid choice. Please enter 1-6."
                    );
            }
        }

        scanner.close();
    }

    private static void printMenu() {

        System.out.println();
        System.out.println("--------------- MENU ----------------");
        System.out.println("1. Scan Directory");
        System.out.println("2. Find Exact Duplicates");
        System.out.println("3. Find Similar Text Files");
        System.out.println("4. View Scan History");
        System.out.println("5. Generate Report");
        System.out.println("6. Exit");
        System.out.println("-------------------------------------");
        System.out.print("Enter your choice: ");
    }

    private static void scanDirectory() {

        System.out.println();
        System.out.print(
                "Enter directory path to scan: "
        );

        String directory =
                scanner.nextLine().trim();

        try {

            currentDirectory =
                    InputValidator
                            .validateDirectory(directory)
                            .toString();

            System.out.println();
            System.out.println(
                    "Scanning directory..."
            );

            currentFiles =
                    scanService.scanDirectory(
                            currentDirectory
                    );

            currentDuplicates =
                    new ArrayList<>();

            currentSimilarities =
                    new ArrayList<>();

            currentScan = null;

            System.out.println();
            System.out.println(
                    "Scan completed successfully."
            );

            System.out.println(
                    "Files found: "
                            + currentFiles.size()
            );

        } catch (InvalidDirectoryException e) {

            System.out.println();
            System.out.println(
                    "Error: " + e.getMessage()
            );
        }
    }

    private static void findExactDuplicates() {

        if (!checkFilesScanned()) {
            return;
        }

        System.out.println();
        System.out.println(
                "Finding exact duplicates..."
        );

        currentDuplicates =
                scanService.findExactDuplicates(
                        currentFiles
                );

        System.out.println();
        System.out.println(
                "Exact duplicate groups found: "
                        + currentDuplicates.size()
        );

        for (DuplicateGroup group :
                currentDuplicates) {

            System.out.println();
            System.out.println(group);

            for (FileMetadata file :
                    group.getFiles()) {

                System.out.println(
                        "  - "
                                + file.getFilePath()
                );
            }
        }

        rebuildCurrentScan();
        saveCurrentScan();
    }

    private static void findSimilarTextFiles() {

        if (!checkFilesScanned()) {
            return;
        }

        System.out.println();
        System.out.print(
                "Enter similarity threshold (0-100): "
        );

        String thresholdInput =
                scanner.nextLine().trim();

        try {

            double threshold =
                    InputValidator.validateThreshold(
                            thresholdInput
                    );

            System.out.println();
            System.out.println(
                    "Finding similar text files..."
            );

            currentSimilarities =
                    scanService.findSimilarTextFiles(
                            currentFiles,
                            threshold
                    );

            System.out.println();
            System.out.println(
                    "Similar pairs found: "
                            + currentSimilarities.size()
            );

            for (SimilarityResult result :
                    currentSimilarities) {

                System.out.println(result);
            }

            rebuildCurrentScan();
            saveCurrentScan();

        } catch (IllegalArgumentException e) {

            System.out.println(
                    "Error: " + e.getMessage()
            );
        }
    }

    private static void viewScanHistory() {

        try {

            List<String> history =
                    scanService.getScanHistory();

            System.out.println();
            System.out.println(
                    "----------- SCAN HISTORY -----------"
            );

            if (history.isEmpty()) {

                System.out.println(
                        "No scan history available."
                );

            } else {

                for (String record : history) {
                    System.out.println(record);
                }
            }

            System.out.println(
                    "------------------------------------"
            );

        } catch (DatabaseOperationException e) {

            System.out.println(
                    "Database error: "
                            + e.getMessage()
            );
        }
    }

    private static void generateReport() {

        if (!checkFilesScanned()) {
            return;
        }

        rebuildCurrentScan();

        reportGenerator.printConsoleReport(
                currentScan
        );

        System.out.println();
        System.out.print(
                "Export report to CSV? (y/n): "
        );

        String answer =
                scanner.nextLine().trim();

        if (answer.equalsIgnoreCase("y")) {

            System.out.print(
                    "Enter CSV file name "
                            + "(default: doppel_report.csv): "
            );

            String fileName =
                    scanner.nextLine().trim();

            if (fileName.isEmpty()) {
                fileName = "doppel_report.csv";
            }

            try {

                reportGenerator.exportCsv(
                        currentScan,
                        fileName
                );

                System.out.println(
                        "CSV report created: "
                                + fileName
                );

            } catch (IOException e) {

                System.out.println(
                        "Unable to create CSV report: "
                                + e.getMessage()
                );
            }
        }
    }

    private static boolean checkFilesScanned() {

        if (currentFiles.isEmpty()) {

            System.out.println();
            System.out.println(
                    "Please scan a directory first using option 1."
            );

            return false;
        }

        return true;
    }

    private static void rebuildCurrentScan() {

        if (currentDirectory == null) {
            return;
        }

        currentScan =
                scanService.createScanResult(
                        currentDirectory,
                        currentFiles,
                        currentDuplicates,
                        currentSimilarities
                );
    }

    private static void saveCurrentScan() {

        if (currentScan == null) {
            return;
        }

        try {

            long scanId =
                    scanService.saveScan(
                            currentScan,
                            currentFiles
                    );

            System.out.println(
                    "Results saved to database. Scan ID: "
                            + scanId
            );

        } catch (DatabaseOperationException e) {

            System.out.println(
                    "Database error: "
                            + e.getMessage()
            );
        }
    }
}
