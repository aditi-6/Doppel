package com.doppel.database;

import com.doppel.exception.DatabaseOperationException;
import com.doppel.model.DuplicateGroup;
import com.doppel.model.FileMetadata;
import com.doppel.model.SimilarityResult;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ScanRepository {

    private final DatabaseManager databaseManager;

    public ScanRepository() {
        databaseManager =
                DatabaseManager.getInstance();
    }

    public long saveScan(
            String directoryPath,
            List<FileMetadata> files,
            List<DuplicateGroup> duplicateGroups,
            List<SimilarityResult> similarityResults)
            throws DatabaseOperationException {

        Connection connection =
                databaseManager.getConnection();

        String scanSql = """
                INSERT INTO scans
                (directory_path, scan_date, total_files)
                VALUES (?, ?, ?)
                """;

        String fileSql = """
                INSERT INTO files
                (scan_id, file_path, file_size, sha256_hash)
                VALUES (?, ?, ?, ?)
                """;

        String duplicateSql = """
                INSERT INTO duplicate_groups
                (scan_id, sha256_hash, file_count)
                VALUES (?, ?, ?)
                """;

        String similaritySql = """
                INSERT INTO similarity_results
                (scan_id, file_a_path, file_b_path, similarity_percent)
                VALUES (?, ?, ?, ?)
                """;

        try {

            connection.setAutoCommit(false);

            long scanId;

            try (PreparedStatement statement =
                         connection.prepareStatement(
                                 scanSql,
                                 Statement.RETURN_GENERATED_KEYS
                         )) {

                statement.setString(1, directoryPath);
                statement.setString(
                        2,
                        LocalDateTime.now().toString()
                );
                statement.setInt(3, files.size());

                statement.executeUpdate();

                try (ResultSet keys =
                             statement.getGeneratedKeys()) {

                    if (!keys.next()) {
                        throw new SQLException(
                                "Unable to obtain scan ID."
                        );
                    }

                    scanId = keys.getLong(1);
                }
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(fileSql)) {

                for (FileMetadata file : files) {

                    statement.setLong(1, scanId);
                    statement.setString(
                            2,
                            file.getFilePath()
                    );
                    statement.setLong(
                            3,
                            file.getFileSize()
                    );
                    statement.setString(
                            4,
                            file.getSha256Hash()
                    );

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(duplicateSql)) {

                for (DuplicateGroup group : duplicateGroups) {

                    statement.setLong(1, scanId);
                    statement.setString(
                            2,
                            group.getSha256Hash()
                    );
                    statement.setInt(
                            3,
                            group.getDuplicateCount()
                    );

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(similaritySql)) {

                for (SimilarityResult result :
                        similarityResults) {

                    statement.setLong(1, scanId);
                    statement.setString(
                            2,
                            result.getFileAPath()
                    );
                    statement.setString(
                            3,
                            result.getFileBPath()
                    );
                    statement.setDouble(
                            4,
                            result.getSimilarityPercentage()
                    );

                    statement.addBatch();
                }

                statement.executeBatch();
            }

            connection.commit();
            connection.setAutoCommit(true);

            return scanId;

        } catch (SQLException e) {

            try {
                connection.rollback();
                connection.setAutoCommit(true);
            } catch (SQLException rollbackException) {
                e.addSuppressed(rollbackException);
            }

            throw new DatabaseOperationException(
                    "Unable to save scan results.",
                    e
            );
        }
    }

    public List<String> getScanHistory()
            throws DatabaseOperationException {

        String sql = """
                SELECT scan_id, directory_path,
                       scan_date, total_files
                FROM scans
                ORDER BY scan_id DESC
                """;

        List<String> history = new ArrayList<>();

        try (PreparedStatement statement =
                     databaseManager
                             .getConnection()
                             .prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {

                long scanId =
                        resultSet.getLong("scan_id");

                String directory =
                        resultSet.getString("directory_path");

                String date =
                        resultSet.getString("scan_date");

                int totalFiles =
                        resultSet.getInt("total_files");

                history.add(
                        String.format(
                                "Scan ID: %d | Directory: %s | Date: %s | Files: %d",
                                scanId,
                                directory,
                                date,
                                totalFiles
                        )
                );
            }

            return history;

        } catch (SQLException e) {

            throw new DatabaseOperationException(
                    "Unable to retrieve scan history.",
                    e
            );
        }
    }
}
