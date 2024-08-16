package ru.infotecs.internship.json;

import ru.infotecs.internship.storage.EnumStorageStatus;

import java.time.LocalDateTime;

/**
 * Represents a basic JSON response object containing an operation status and a timestamp.
 */
public class JsonResponse {

    /**
     * Represents the status of the operation.
     */
    private EnumStorageStatus status;

    /**
     * Stores the string representation of the time the response was created.
     */
    private String timestamp;

    /**
     * Default constructs a new {@code JsonResponse}.
     * Needs for correct deserialization by Jackson library.
     */
    public JsonResponse() {
    }

    /**
     * Constructs a new {@code JsonResponse} with the given status.
     * The timestamp is set to the current time.
     *
     * @param status the status of the response, indicating the result of user operation.
     */
    public JsonResponse(EnumStorageStatus status) {
        this.status = status;
        this.timestamp = LocalDateTime.now().toString();
    }

    /**
     * Gets the status of the response.
     *
     * @return the status of the response.
     */
    public EnumStorageStatus getStatus() {
        return status;
    }

    /**
     * Sets the status of the response.
     *
     * @param status the status to be set for the response.
     */
    public void setStatus(EnumStorageStatus status) {
        this.status = status;
    }

    /**
     * Gets the timestamp of when the response was generated.
     *
     * @return the timestamp of the response.
     */
    public String getTimestamp() {
        return timestamp;
    }

    /**
     * Sets the timestamp for when the response was generated.
     *
     * @param timestamp the timestamp to set for the response.
     */
    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
