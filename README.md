# Doppel — Multithreaded File and Text Duplicate Detection System

A command-line Java application that scans a directory tree, identifies exact duplicate files using SHA-256 hashing, detects near-duplicate text files using Jaccard similarity, persists scan history to a SQLite database via JDBC, and generates console and CSV reports.

## Overview

Duplicate and near-duplicate files accumulate silently in most working directories — repeated downloads, copy-pasted notes, redundant project exports. Doppel was built as an academic exercise in applying core Java concepts (OOP, collections, exception handling, multithreading, and JDBC) to a genuinely useful problem: scanning a directory, finding files that are byte-for-byte identical, and separately flagging text files that are similar but not identical.

The project is intentionally scoped as a terminal application with no GUI, no web framework, and no external services, so that its behavior can be fully inspected and explained from the source code and executed directly from a command line.

## Objectives

- Recursively scan a directory and collect file metadata without assuming file names indicate duplication.
- Detect exact duplicates efficiently using a size-based prefilter followed by SHA-256 hashing.
- Detect textual similarity between text files using a transparent, explainable algorithm (Jaccard similarity).
- Demonstrate meaningful use of concurrency for independent, parallelizable file-processing work.
- Persist scan results for later review using a relational database accessed through JDBC.
- Present results through a clear CLI and export them to CSV when needed.

## Key Features

- Recursive directory scanning with graceful handling of inaccessible files.
- Exact duplicate detection via SHA-256, with a file-size prefilter to avoid unnecessary hashing.
- Text similarity detection via Jaccard similarity with a user-configurable threshold.
- Multithreaded file processing using `ExecutorService`, with thread-safe result aggregation.
- SQLite persistence of scan history, discovered files, duplicate groups, and similarity results.
- Console reporting and optional CSV export.
- Input validation for directory paths and similarity thresholds.
- Custom checked exceptions for invalid directories and database failures.

## System Architecture

Doppel follows a layered flow: user input is validated, the directory is scanned for metadata, detection algorithms run over the collected metadata, results are aggregated into a single `ScanResult`, and that result is both persisted and reported.

```
                 ┌────────────────┐
   User Input →  │ InputValidator │
                 └────────┬───────┘
                          ▼
                 ┌────────────────────┐
                 │   DirectoryScanner │  (recursive discovery)
                 └────────┬───────────┘
                          ▼
                 List<FileMetadata>
                          │
        ┌─────────────────┴────────────────────┐
        ▼                                      ▼
┌──────────────────┐                  ┌────────────────────┐
│ DuplicateDetector│                  │  JaccardSimilarity │
│ (size → SHA-256) │                  │  (text file pairs) │
└─────────┬────────┘                  └──────────┬─────────┘
          ▼                                      ▼
  List<DuplicateGroup>                 List<SimilarityResult>
          └───────────────┬──────────────────────┘
                           ▼
                     ┌────────────┐
                     │ ScanResult │
                     └─────┬──────┘
                           │
              ┌────────────┴────────────┐
              ▼                         ▼
     ┌──────────────────┐     ┌────────────────────┐
     │ ScanRepository   │     │  ReportGenerator   │
     │ (SQLite via JDBC)│     │  (console / CSV)   │
     └──────────────────┘     └────────────────────┘
```

`ScanService` is the orchestrator that wires these stages together and is what `Main` calls into from the CLI.

## Core Algorithms

### Exact Duplicate Detection

1. **File discovery** — `DirectoryScanner` recursively walks the target directory using `Files.walkFileTree`, collecting a `FileMetadata` record (path, size, last modified) for every regular file it can access.
2. **Size prefilter** — `DuplicateDetector` first groups all discovered files by file size. Files with a unique size in the directory cannot have a duplicate and are excluded from hashing immediately.
3. **SHA-256 hashing** — Only files that share a size with at least one other file are hashed, using `HashCalculator`, which reads each file through a buffered `InputStream` and produces a lowercase hexadecimal SHA-256 digest via `MessageDigest`.
4. **Grouping** — Files are then grouped by the combination of size and hash. Any group containing more than one file is emitted as a `DuplicateGroup`.

The size prefilter exists because hashing is the expensive step: reading and digesting full file contents is unnecessary for files that are already provably distinct by size. Filtering by size first reduces the number of files that need to be hashed, which matters more as the scanned directory grows.

### Jaccard Similarity

For text files (as identified by extension, see `FileUtils`), `JaccardSimilarity` implements the `SimilarityStrategy` interface:

- Text is lowercased and stripped of characters that are not letters or digits.
- The cleaned text is tokenized on whitespace into a set of unique tokens.
- Similarity between two files is computed as:

```
J(A, B) = |A ∩ B| / |A ∪ B|
```

where `A` and `B` are the token sets of the two files. The result is expressed as a percentage. Empty token sets are handled explicitly to avoid division by zero. A pair is reported only if its similarity meets or exceeds the user-configured threshold.

## Multithreading and Concurrency

Concurrency in Doppel is applied specifically to independent, per-file processing work — not to the recursive directory traversal itself, which is inherently sequential (each directory must be visited to discover its contents).

- **`DirectoryScanner`** discovers files sequentially via `Files.walkFileTree`, but dispatches the metadata-collection work for each discovered file to an `ExecutorService`. Results are collected into a `ConcurrentLinkedQueue`, which allows worker threads to append results without external synchronization, before being sorted by path.
- **`DuplicateDetector`** submits SHA-256 hash computation for each size-candidate file to an `ExecutorService`, since hashing one file is completely independent of hashing another. Intermediate grouping structures use `ConcurrentHashMap` (for size/hash buckets being built concurrently) and `CopyOnWriteArrayList` (for group membership lists that are read far more often than they are mutated).

In short: **file discovery (the directory walk) is sequential; per-file work (metadata collection, hashing) is parallelized** across worker threads, with thread-safe collections used specifically where multiple threads write to a shared structure.

## Object-Oriented Design

- **Encapsulation** — Model classes (`FileMetadata`, `ScanResult`, `DuplicateGroup`, `SimilarityResult`) expose data only through constructors and getters. `ScanResult` exposes final state and unmodifiable result lists, reflecting that a completed scan's own fields should not change after the fact.
- **Separation of responsibilities** — Each package has a single concern: `scanner` only discovers files, `detector` only identifies duplicates, `similarity` only compares text, `database` only persists, `report` only formats output. `ScanService` is the only class that coordinates across these boundaries.
- **Interface and polymorphism** — `SimilarityStrategy` defines a single method, `double compare(String textA, String textB)`. `JaccardSimilarity` is its only current implementation, but any code that depends on `SimilarityStrategy` (such as `ScanService`) can work with any future implementation (for example, a cosine-similarity or edit-distance strategy) without modification. This is a concrete, minimal use of the Strategy pattern rather than inheritance added for its own sake.
- **Custom exceptions** — `InvalidDirectoryException` and `DatabaseOperationException` extend the standard exception hierarchy to give callers specific, catchable failure types instead of generic exceptions.

## Database Design

Doppel uses SQLite through JDBC (`jdbc:sqlite:doppel.db`), which requires no separate database server and keeps the project fully offline after the JDBC driver is available locally.

| Table | Purpose |
|---|---|
| `scans` | One row per scan run: directory path, timestamp, total files scanned. |
| `files` | File metadata (path, size, SHA-256 hash) linked to the scan that discovered it. |
| `duplicate_groups` | One row per group of files sharing a hash, linked to the scan. |
| `similarity_results` | One row per text file pair that met the similarity threshold, linked to the scan. |

`files`, `duplicate_groups`, and `similarity_results` each reference `scans` by scan ID, associating each of these result tables with the specific scan operation that produced them. Because the CLI can persist exact-duplicate detection and similarity analysis as separate scan records, a given `scans` row may correspond to one type of analysis rather than a single combined run covering both. `ScanRepository` performs all writes through `PreparedStatement`s, uses transactions and batch inserts when persisting a scan's files, and exposes scan history retrieval for past scans.

## CLI Workflow

```
Doppel
1. Scan Directory
2. Find Exact Duplicates
3. Find Similar Text Files
4. View Scan History
5. Generate Report
6. Exit
```

| Option | Behavior |
|---|---|
| Scan Directory | Prompts for a directory path, validates it, and runs `DirectoryScanner` to collect file metadata. |
| Find Exact Duplicates | Runs `DuplicateDetector` over the most recent scan's files and displays duplicate groups. |
| Find Similar Text Files | Prompts for a similarity threshold and runs `JaccardSimilarity` over text files from the most recent scan. |
| View Scan History | Retrieves and displays previously persisted scans from the database. |
| Generate Report | Produces a console summary and/or writes a CSV report of the current scan's results. |
| Exit | Terminates the application. |

## Project Structure

```
Doppel/
├── docs/
├── lib/
│   └── sqlite-jdbc-3.53.4.0.jar
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── doppel/
│                   ├── database/
│                   │   ├── DatabaseManager.java
│                   │   └── ScanRepository.java
│                   ├── detector/
│                   │   ├── DuplicateDetector.java
│                   │   └── HashCalculator.java
│                   ├── exception/
│                   │   ├── DatabaseOperationException.java
│                   │   └── InvalidDirectoryException.java
│                   ├── model/
│                   │   ├── DuplicateGroup.java
│                   │   ├── FileMetadata.java
│                   │   ├── ScanResult.java
│                   │   └── SimilarityResult.java
│                   ├── report/
│                   │   └── ReportGenerator.java
│                   ├── scanner/
│                   │   └── DirectoryScanner.java
│                   ├── service/
│                   │   └── ScanService.java
│                   ├── similarity/
│                   │   ├── JaccardSimilarity.java
│                   │   └── SimilarityStrategy.java
│                   ├── util/
│                   │   ├── FileUtils.java
│                   │   └── InputValidator.java
│                   └── Main.java
├── test-data/
├── .gitignore
└── README.md
```

`out/`, `doppel.db`, and `doppel_report.csv` are generated at build/run time and are not part of the source tree.

## Input Validation and Error Handling

- **Invalid directory** — `InputValidator` checks that a supplied path exists and is a directory before scanning begins. An invalid path raises `InvalidDirectoryException` and is reported to the user without crashing the application.
- **Inaccessible files** — `DirectoryScanner` handles individual file access failures during the walk (via `visitFileFailed`) rather than aborting the entire scan.
- **Invalid similarity threshold** — `InputValidator` requires the threshold to be a number between 0 and 100. A non-numeric or out-of-range entry produces a clear error message rather than an exception trace.
- **Database errors** — Failures in `DatabaseManager` or `ScanRepository` are wrapped in `DatabaseOperationException` so calling code can handle persistence failures distinctly from other errors.

## Reporting

- **Console report** — `ReportGenerator` prints a scan summary, the list of exact duplicate groups, and the list of similar text file pairs directly to the terminal.
- **CSV report** — `ReportGenerator` can also write the same results to a CSV file (`doppel_report.csv`) for external review.

## Testing and Verification

Testing was performed manually against a small controlled test directory.

**Test data:**

| File | Content |
|---|---|
| `file1.txt` | "hello world this is a duplicate detection test" |
| `file2.txt` | Exact copy of `file1.txt` |
| `file3.txt` | "hello world this is a similarity detection test" |
| `file4.txt` | "completely different content for testing" |

**Results:**

| Test | Outcome |
|---|---|
| Directory scan | 4 files found |
| Exact duplicate detection | 1 duplicate group identified: `file1.txt` and `file2.txt` (48 bytes each, identical SHA-256 hash) |
| Similarity detection (threshold 50%) | `file1` ↔ `file2` = 100.00%; `file1` ↔ `file3` = 77.78%; `file2` ↔ `file3` = 77.78%; `file4` not reported (below threshold) |
| Scan history | Two prior scans (Scan ID 1, Scan ID 2) retrieved and displayed correctly |
| CSV export | `doppel_report.csv` generated and verified |
| Invalid directory input | Nonexistent path handled with an error message, no crash |
| Invalid threshold input | Entering `abc` produced: "Threshold must be a number between 0 and 100." |

## How to Run

### Prerequisites

- JDK installed (developed and tested on JDK 26; written using syntax compatible with JDK 17+).
- SQLite JDBC driver JAR (`sqlite-jdbc-3.53.4.0.jar`), placed in `lib/`.

### Compilation (Windows PowerShell)

```powershell
javac -d out (Get-ChildItem -Recurse -Filter *.java | ForEach-Object { $_.FullName })
```

### Execution

```powershell
java -cp "out;lib\sqlite-jdbc-3.53.4.0.jar" com.doppel.Main
```

### Example Usage

1. Run the application and choose option `1` to scan a directory.
2. Enter a valid directory path when prompted.
3. Choose option `2` to view exact duplicate groups from that scan.
4. Choose option `3`, enter a similarity threshold (e.g. `50`), to view similar text file pairs.
5. Choose option `5` to generate a console and/or CSV report.
6. Choose option `4` at any point to review past scans stored in the database.

## Design Decisions

- **SHA-256** was chosen for exact duplicate detection because it is a widely available, collision-resistant hash accessible directly through `java.security.MessageDigest`, requiring no external dependency.
- **Size prefilter before hashing** avoids reading and digesting full file contents for files that cannot possibly be duplicates, reducing unnecessary I/O.
- **Jaccard similarity** was chosen over more complex similarity measures because its logic (set intersection over union) is simple to implement correctly, explain, and verify by hand — appropriate for an academic project where the algorithm itself needs to be understood, not just used.
- **`ExecutorService`** was used instead of manually managed `Thread` objects to make thread pool sizing and task submission explicit and easier to reason about, while still directly demonstrating thread creation, task execution, and shared-state synchronization concepts from the course syllabus.
- **SQLite** was chosen over a client-server database because it requires no separate installation or running service, keeping the project runnable entirely offline from a single JAR dependency.
- **`SimilarityStrategy` as an interface** keeps `ScanService` decoupled from the specific similarity algorithm in use, so a different algorithm could be substituted without changing the code that calls it.

## Limitations

- Text-file detection relies on file extension (`FileUtils`) and does not inspect file content to confirm it is genuinely text; a file with a text extension but binary content may be misprocessed.
- Similarity comparison is currently limited to text files as identified by extension; binary near-duplicate detection is not implemented.
- The application has been tested manually against a small, controlled dataset; it has not been benchmarked against very large directories or high file counts.

## Future Scope

The following are potential directions beyond the current implementation and are not currently built:

- Additional `SimilarityStrategy` implementations (e.g. cosine similarity, edit distance) alongside Jaccard.
- Content-based (rather than extension-based) detection of text versus binary files.
- Configurable thread pool sizing exposed through the CLI.
- A richer report format (e.g. HTML) in addition to console and CSV output.

## Academic/Technical Summary

Doppel demonstrates recursive file-system traversal, encapsulated data modeling, interface-based polymorphism via the Strategy pattern, custom checked exceptions, concurrent task execution with thread-safe shared collections, and relational persistence through JDBC with prepared statements and transactions — applied to a single coherent problem: identifying exact and near-duplicate files in a directory from the command line.
