package com.doppel.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String DATABASE_URL =
            "jdbc:sqlite:doppel.db";

    private static DatabaseManager instance;

    private Connection connection;

    private DatabaseManager() {
        initializeConnection();
        initializeDatabase();
    }

    public static synchronized DatabaseManager getInstance() {

        if (instance == null) {
            instance = new DatabaseManager();
        }

        return instance;
    }

    private void initializeConnection() {

        try {
            connection =
                    DriverManager.getConnection(DATABASE_URL);

        } catch (SQLException e) {
            throw new RuntimeException(
                    "Unable to connect to SQLite database.",
                    e
            );
        }
    }

    public synchronized Connection getConnection() {

        try {

            if (connection == null ||
                    connection.isClosed()) {

                initializeConnection();
            }

            return connection;

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to access database connection.",
                    e
            );
        }
    }

    private void initializeDatabase() {

        String scansTable = """
                CREATE TABLE IF NOT EXISTS scans (
                    scan_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    directory_path TEXT NOT NULL,
                    scan_date TEXT NOT NULL,
                    total_files INTEGER NOT NULL
                )
                """;

        String filesTable = """
                CREATE TABLE IF NOT EXISTS files (
                    file_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scan_id INTEGER NOT NULL,
                    file_path TEXT NOT NULL,
                    file_size INTEGER NOT NULL,
                    sha256_hash TEXT,
                    FOREIGN KEY (scan_id)
                        REFERENCES scans(scan_id)
                )
                """;

        String duplicateGroupsTable = """
                CREATE TABLE IF NOT EXISTS duplicate_groups (
                    group_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scan_id INTEGER NOT NULL,
                    sha256_hash TEXT NOT NULL,
                    file_count INTEGER NOT NULL,
                    FOREIGN KEY (scan_id)
                        REFERENCES scans(scan_id)
                )
                """;

        String similarityTable = """
                CREATE TABLE IF NOT EXISTS similarity_results (
                    result_id INTEGER PRIMARY KEY AUTOINCREMENT,
                    scan_id INTEGER NOT NULL,
                    file_a_path TEXT NOT NULL,
                    file_b_path TEXT NOT NULL,
                    similarity_percent REAL NOT NULL,
                    FOREIGN KEY (scan_id)
                        REFERENCES scans(scan_id)
                )
                """;

        try (Statement statement =
                     getConnection().createStatement()) {

            statement.execute(scansTable);
            statement.execute(filesTable);
            statement.execute(duplicateGroupsTable);
            statement.execute(similarityTable);

        } catch (SQLException e) {

            throw new RuntimeException(
                    "Unable to initialize database tables.",
                    e
            );
        }
    }

    public synchronized void close() {

        if (connection != null) {

            try {
                connection.close();
            } catch (SQLException e) {
                System.out.println(
                        "Unable to close database connection."
                );
            }
        }
    }
}
