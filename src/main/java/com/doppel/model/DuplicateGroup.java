package com.doppel.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DuplicateGroup {

    private final String sha256Hash;
    private final long fileSize;
    private final List<FileMetadata> files;

    public DuplicateGroup(
            String sha256Hash,
            long fileSize,
            List<FileMetadata> files) {

        this.sha256Hash = sha256Hash;
        this.fileSize = fileSize;

        this.files = Collections.unmodifiableList(
                new ArrayList<>(files)
        );
    }

    public String getSha256Hash() {
        return sha256Hash;
    }

    public long getFileSize() {
        return fileSize;
    }

    public List<FileMetadata> getFiles() {
        return files;
    }

    public int getDuplicateCount() {
        return files.size();
    }

    @Override
    public String toString() {
        String shortHash = sha256Hash.length() >= 8
                ? sha256Hash.substring(0, 8)
                : sha256Hash;

        return "DuplicateGroup{" +
                "hash='" + shortHash + "...'" +
                ", count=" + files.size() +
                ", size=" + fileSize +
                " bytes}";
    }
}
