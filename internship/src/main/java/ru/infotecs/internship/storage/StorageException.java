package ru.infotecs.internship.storage;

/**
 * Custom exception class to use in JsonProcessor and StorageDriver.
 */
public class StorageException extends Exception {

    /**
     * Exception constructor.
     *
     * @param message string containing error description
     */
    public StorageException(String message) {
        super(message);
    }
}
