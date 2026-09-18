package com.doppel.model;

public class FileMetadata {

    private final String filePath;
    private final long fileSize;
    private final long lastModified;
    private String sha256Hash;

    public FileMetadata(String filePath, long fileSize, long lastModified) {
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.lastModified = lastModified;
        this.sha256Hash = null;
    }

    public String getFilePath() {
        return filePath;
    }

    public long getFileSize() {
        return fileSize;
    }

    public long getLastModified() {
        return lastModified;
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public void setSha256Hash(String sha256Hash) {
        this.sha256Hash = sha256Hash;
    }

    @Override
    public String toString() {
        return "FileMetadata{" +
                "path='" + filePath + '\'' +
                ", size=" + fileSize +
                " bytes}";
    }
}
