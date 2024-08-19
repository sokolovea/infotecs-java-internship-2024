package ru.infotecs.internship.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

/**
 * Represents a record with a value and an expiration time.
 */
public class RecordValue implements Serializable {

    /**
     * The value of the record.
     */
    @JsonProperty("value")
    private String value;

    /**
     * The expiration time of the record in milliseconds since the epoch.
     * It represents the time when the record will expire.
     */
    @JsonProperty("expirationTime")
    private Long expirationTime;

    /**
     * Default constructor.
     */
    public RecordValue() {
    }

    /**
     * Constructs a RecordValue object with the given value and TTL in milliseconds.
     *
     * @param value the value of the record
     * @param ttlMs the time to live in milliseconds
     */
    public RecordValue(String value, Long ttlMs) {
        this.value = value;
        setTtlMs(ttlMs);
    }

    /**
     * Gets the value of the record.
     *
     * @return the value of the record
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the record.
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the expiration time of the record.
     *
     * @return the expiration time in milliseconds since epoch, or null if it is not set by user
     */
    public Long getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets the expiration time of the record.
     *
     * @param expirationTime the expiration time in milliseconds since epoch
     */
    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Gets the TTL of the record in milliseconds.
     * TTL is the difference between the expiration time and the current time.
     *
     * @return the TTL in milliseconds, or null if the expiration time is not set
     */
    public Long getTtlMs() {
        if (expirationTime == null) {
            return null;
        }
        return expirationTime - System.currentTimeMillis();
    }

    /**
     * Sets the TTL of the record.
     *
     * @param ttlMs the TTL in milliseconds
     */
    public void setTtlMs(long ttlMs) {
        this.expirationTime = System.currentTimeMillis() + ttlMs;
    }
}
