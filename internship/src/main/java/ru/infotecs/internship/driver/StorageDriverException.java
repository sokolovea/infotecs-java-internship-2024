package ru.infotecs.internship.driver;

/**
 * Custom exception class to use in StorageDriver.
 */
public class StorageDriverException extends Exception {

    /**
     * Exception constructor.
     *
     * @param message string containing error description
     */
    public StorageDriverException(String message) {
        super(message);
    }
}
