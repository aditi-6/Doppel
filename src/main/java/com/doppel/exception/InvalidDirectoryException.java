package com.doppel.exception;

public class InvalidDirectoryException extends Exception {

    public InvalidDirectoryException(String message) {
        super(message);
    }

    public InvalidDirectoryException(String message, Throwable cause) {
        super(message, cause);
    }
}
