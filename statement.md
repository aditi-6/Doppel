# Project Statement

## Problem Statement

Managing large directories often results in multiple copies of the same file and slightly modified versions of text files. Manually identifying exact duplicates and similar text files is time-consuming and inefficient.

Doppel is designed to address this problem by scanning a directory recursively, collecting file metadata, detecting exact duplicates using SHA-256 hashing, and identifying similar text files using Jaccard similarity. The system provides the results through a command-line interface and stores scan information using SQLite for future reference.

## Scope of the Project

The scope of Doppel includes:

- Recursive scanning of directories and subdirectories.
- Collection of file metadata such as file path, file size, and modification time.
- Detection of exact duplicate files using file-size comparison and SHA-256 hashing.
- Detection of similar text files using Jaccard similarity.
- Configurable similarity threshold for text comparison.
- Multithreaded file processing using Java ExecutorService.
- Storage of scan results and history using SQLite.
- Generation of console reports and optional CSV reports.
- Validation and handling of invalid directories, inaccessible files, invalid thresholds, and database errors.

The project is implemented as a Java command-line application and does not include a graphical interface, web application, or cloud-based processing.

## Target Users

Doppel is intended for users who need to analyze and organize files stored in local directories, including:

- Students managing project and academic files.
- Developers managing source-code directories and project files.
- Users maintaining folders containing multiple versions or copies of files.
- Anyone who needs a command-line tool for duplicate and text similarity analysis.

## High-Level Features

1. **Directory Scanning**
   - Recursively scans a selected directory.
   - Collects metadata for accessible regular files.
   - Uses multithreaded processing for file metadata collection.

2. **Exact Duplicate Detection**
   - Groups files by file size as a prefilter.
   - Calculates SHA-256 hashes for candidate files.
   - Identifies files with matching size and hash as exact duplicates.

3. **Similar Text Detection**
   - Identifies supported text files.
   - Normalizes and tokenizes their contents.
   - Calculates Jaccard similarity between text files.
   - Reports pairs meeting the user-defined similarity threshold.

4. **Database Persistence**
   - Stores scan operations and file information in SQLite.
   - Maintains duplicate-group and similarity-result records.
   - Provides scan history through the command-line interface.

5. **Reporting**
   - Displays scan results through the console.
   - Shows duplicate groups and similar text pairs.
   - Supports exporting results to CSV.

6. **Input Validation and Error Handling**
   - Validates directory paths and similarity thresholds.
   - Handles inaccessible files and database operation errors.
