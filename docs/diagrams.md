# Doppel — Design Diagrams

This document contains the design diagrams for the Doppel project: system architecture, workflow, use case, class diagram, sequence diagram, and ER diagram. All diagrams are written in Mermaid syntax and render automatically on GitHub. Class names, method signatures, and relationships reflect the actual Java source code.

## 1. System Architecture Diagram

```mermaid
flowchart TD
    User([User - CLI Input]) --> Main[Main]

    Main --> IV[InputValidator]
    Main --> SS[ScanService]
    Main --> RG[ReportGenerator]

    SS --> DS[DirectoryScanner]
    SS --> DD[DuplicateDetector]
    SS --> SIM[SimilarityStrategy / JaccardSimilarity]
    SS --> SR[ScanRepository]
    SS --> FU[FileUtils]

    DD --> HC[HashCalculator]

    SR --> DM[DatabaseManager]
    DM --> DB[(SQLite)]

    RG --> FU
```

Main directly coordinates `InputValidator`, `ScanService`, and `ReportGenerator`. `ScanService` is the only class that coordinates `DirectoryScanner`, `DuplicateDetector`, the similarity strategy, `ScanRepository`, and `FileUtils`; these are not called directly by `Main`.

## 2. Workflow / Process Flow Diagram

```mermaid
flowchart TD
    Start([Start]) --> Menu{CLI Menu}

    Menu -->|1| ScanOpt[Scan Directory]
    Menu -->|2| DupOpt[Find Exact Duplicates]
    Menu -->|3| SimOpt[Find Similar Text Files]
    Menu -->|4| HistOpt[View Scan History]
    Menu -->|5| RepOpt[Generate Report]
    Menu -->|6| ExitOpt([Exit])

    ScanOpt --> AskDir[Prompt for directory path]
    AskDir --> ValDir[InputValidator.validateDirectory]
    ValDir -->|InvalidDirectoryException| ErrDir[Show error]
    ErrDir --> Menu
    ValDir -->|valid Path| CallScan[ScanService.scanDirectory]
    CallScan --> DoScan[DirectoryScanner.scan]
    DoScan --> UpdateFiles[Update currentFiles in Main]
    UpdateFiles --> Menu

    DupOpt --> CheckFiles1{currentFiles populated?}
    CheckFiles1 -->|no| ErrNoScan1[Prompt to scan first]
    ErrNoScan1 --> Menu
    CheckFiles1 -->|yes| CallDup[ScanService.findExactDuplicates]
    CallDup --> DoDup[DuplicateDetector.findDuplicates]
    DoDup --> UpdateDup[Update currentDuplicates]
    UpdateDup --> Rebuild1[rebuildCurrentScan]
    Rebuild1 --> SaveCall1[saveCurrentScan]
    SaveCall1 --> SaveScan1[ScanService.saveScan]
    SaveScan1 --> RepoSave1[ScanRepository.saveScan]
    RepoSave1 --> DB1[(SQLite)]
    DB1 --> Menu

    SimOpt --> CheckFiles2{currentFiles populated?}
    CheckFiles2 -->|no| ErrNoScan2[Prompt to scan first]
    ErrNoScan2 --> Menu
    CheckFiles2 -->|yes| AskThresh[Prompt for threshold]
    AskThresh --> ValThresh[InputValidator.validateThreshold]
    ValThresh -->|invalid| ErrThresh[Show error]
    ErrThresh --> Menu
    ValThresh -->|valid double| CallSim[ScanService.findSimilarTextFiles]
    CallSim --> UseFileUtils[FileUtils identifies/reads text files]
    UseFileUtils --> CallJaccard[SimilarityStrategy.compare - JaccardSimilarity]
    CallJaccard --> UpdateSim[Update currentSimilarities]
    UpdateSim --> Rebuild2[rebuildCurrentScan]
    Rebuild2 --> SaveCall2[saveCurrentScan]
    SaveCall2 --> SaveScan2[ScanService.saveScan]
    SaveScan2 --> RepoSave2[ScanRepository.saveScan]
    RepoSave2 --> DB2[(SQLite)]
    DB2 --> Menu

    HistOpt --> GetHist[ScanService.getScanHistory]
    GetHist --> RepoHist[ScanRepository.getScanHistory]
    RepoHist --> DB3[(SQLite)]
    DB3 --> Menu

    RepOpt --> Rebuild3[rebuildCurrentScan]
    Rebuild3 --> Console[ReportGenerator.printConsoleReport]
    Console --> CsvOpt{Export CSV?}
    CsvOpt -->|yes| Csv[ReportGenerator.exportCsv]
    CsvOpt -->|no| Menu
    Csv --> Menu
```

Scanning a directory (option 1) only populates `currentFiles`; it does not automatically run duplicate detection, similarity analysis, persistence, or reporting. Those actions occur only when the corresponding menu option is selected.

## 3. Use Case Diagram

```mermaid
flowchart LR
    User((User))

    User --> UC1[Scan Directory]
    User --> UC2[Find Exact Duplicates]
    User --> UC3[Find Similar Text Files]
    User --> UC4[View Scan History]
    User --> UC5[Generate Report]
    User --> UC6[Exit]

    UC1 -.includes.-> I1[Validate Directory Path]
    UC3 -.includes.-> I2[Validate Similarity Threshold]
    UC2 -.includes.-> I3[Persist Scan Results]
    UC3 -.includes.-> I3
    UC4 -.includes.-> I4[Retrieve Scan History]
    UC5 -.includes.-> I5[Export CSV Report]
```

Validation, persistence, history retrieval, and CSV export are internal supporting interactions triggered as part of a primary use case; the user does not invoke them directly.

## 4. Class Diagram

```mermaid
classDiagram
    class FileMetadata {
        -String filePath
        -long fileSize
        -long lastModified
        -String sha256Hash
        +getFilePath() String
        +getFileSize() long
        +getLastModified() long
        +getSha256Hash() String
        +setSha256Hash(String) void
        +toString() String
    }

    class ScanResult {
        -String scannedDirectory
        -int totalFilesScanned
        -List~DuplicateGroup~ duplicateGroups
        -List~SimilarityResult~ similarityResults
        +getScannedDirectory() String
        +getTotalFilesScanned() int
        +getDuplicateGroups() List~DuplicateGroup~
        +getSimilarityResults() List~SimilarityResult~
        +toString() String
    }

    class DuplicateGroup {
        -String sha256Hash
        -long fileSize
        -List~FileMetadata~ files
        +getSha256Hash() String
        +getFileSize() long
        +getFiles() List~FileMetadata~
        +getDuplicateCount() int
        +toString() String
    }

    class SimilarityResult {
        -String fileAPath
        -String fileBPath
        -double similarityPercentage
        +getFileAPath() String
        +getFileBPath() String
        +getSimilarityPercentage() double
        +toString() String
    }

    class DirectoryScanner {
        +scan(String directoryPath) List~FileMetadata~
    }

    class HashCalculator {
        +calculateSHA256(Path file) String
    }

    class DuplicateDetector {
        +findDuplicates(List~FileMetadata~) List~DuplicateGroup~
    }

    class SimilarityStrategy {
        <<interface>>
        +compare(String textA, String textB) double
    }

    class JaccardSimilarity {
        +compare(String textA, String textB) double
    }

    class DatabaseManager {
        -String DATABASE_URL
        -DatabaseManager instance
        -Connection connection
        +getInstance() DatabaseManager
        +getConnection() Connection
        +close() void
    }

    class ScanRepository {
        +saveScan(String directoryPath, List~FileMetadata~ files, List~DuplicateGroup~ duplicateGroups, List~SimilarityResult~ similarityResults) long
        +getScanHistory() List~String~
    }

    class ScanService {
        +scanDirectory(String directoryPath) List~FileMetadata~
        +findExactDuplicates(List~FileMetadata~) List~DuplicateGroup~
        +findSimilarTextFiles(List~FileMetadata~, double) List~SimilarityResult~
        +createScanResult(String directoryPath, List~FileMetadata~, List~DuplicateGroup~, List~SimilarityResult~) ScanResult
        +saveScan(ScanResult, List~FileMetadata~) long
        +getScanHistory() List~String~
    }

    class ReportGenerator {
        +printConsoleReport(ScanResult) void
        +exportCsv(ScanResult, String) void
    }

    class InputValidator {
        +validateDirectory(String directory) Path
        +validateThreshold(String input) double
    }

    class FileUtils {
        +isTextFile(String) boolean
        +readTextFile(String) String
        +escapeCsv(String) String
    }

    class InvalidDirectoryException {
        <<exception>>
    }

    class DatabaseOperationException {
        <<exception>>
    }

    SimilarityStrategy <|.. JaccardSimilarity
    DuplicateDetector --> HashCalculator
    DuplicateDetector ..> DuplicateGroup : produces
    DirectoryScanner ..> FileMetadata : produces
    DirectoryScanner ..> InvalidDirectoryException : throws
    ScanService --> DirectoryScanner
    ScanService --> DuplicateDetector
    ScanService --> SimilarityStrategy
    ScanService --> ScanRepository
    ScanService --> FileUtils
    ScanService ..> ScanResult : creates
    ScanRepository --> DatabaseManager
    ScanRepository ..> DatabaseOperationException : throws
    ScanResult --> DuplicateGroup
    ScanResult --> SimilarityResult
    DuplicateGroup --> FileMetadata
    ReportGenerator --> FileUtils
```

## 5. Sequence Diagram — Find Exact Duplicates (after a directory has already been scanned)

```mermaid
sequenceDiagram
    actor User
    participant Main
    participant ScanService
    participant DuplicateDetector
    participant HashCalculator
    participant ScanRepository
    participant DatabaseManager

    User->>Main: Select "Find Exact Duplicates"
    Main->>Main: Check currentFiles is populated
    Main->>ScanService: findExactDuplicates(currentFiles)
    ScanService->>DuplicateDetector: findDuplicates(currentFiles)
    DuplicateDetector->>HashCalculator: calculateSHA256(Path)
    HashCalculator-->>DuplicateDetector: hash String
    DuplicateDetector-->>ScanService: List<DuplicateGroup>
    ScanService-->>Main: List<DuplicateGroup>
    Main->>Main: Update currentDuplicates
    Main->>Main: rebuildCurrentScan()
    Main->>Main: saveCurrentScan()
    Main->>ScanService: saveScan(scanResult, currentFiles)
    ScanService->>ScanRepository: saveScan(directoryPath, files, duplicateGroups, similarityResults)
    ScanRepository->>DatabaseManager: getConnection()
    DatabaseManager-->>ScanRepository: Connection
    ScanRepository-->>ScanService: scanId (long)
    ScanService-->>Main: scanId (long)
    Main-->>User: Display duplicate groups
```

This diagram covers only the "Find Exact Duplicates" menu option. Report generation (`ReportGenerator`) is a separate flow triggered by menu option 5 and is not part of this sequence.

## 6. ER Diagram

```mermaid
erDiagram
    SCANS ||--o{ FILES : "referenced by"
    SCANS ||--o{ DUPLICATE_GROUPS : "referenced by"
    SCANS ||--o{ SIMILARITY_RESULTS : "referenced by"

    SCANS {
        int scan_id PK
        string directory_path
        string scan_date
        int total_files
    }

    FILES {
        int file_id PK
        int scan_id FK
        string file_path
        long file_size
        string sha256_hash
    }

    DUPLICATE_GROUPS {
        int group_id PK
        int scan_id FK
        string sha256_hash
        int file_count
    }

    SIMILARITY_RESULTS {
        int result_id PK
        int scan_id FK
        string file_a_path
        string file_b_path
        double similarity_percent
    }
```

Each `scans` row represents one scan operation (a directory scan, a duplicate-detection run, or a similarity-analysis run, since each of these can independently call `saveScan`). `files`, `duplicate_groups`, and `similarity_results` are child records that reference the specific scan operation that produced them; a single `scans` row does not necessarily represent every possible type of analysis for a directory.
