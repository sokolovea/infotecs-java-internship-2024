package ru.infotecs.internship.json;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Basic JSON request to server (set value by key)
 */
public class JsonRequest {
    /**
     * Key associated with record.
     */
    @JsonProperty("key")
    private String key = null;

    /**
     * Record value.
     */
    @JsonProperty("value")
    private String value = null;

    /**
     * Ttl for record.
     */
    @JsonProperty("ttl")
    private Long ttlSeconds = null;


    /**
     * Default constructor. Needs for Jackson.
     */
    public JsonRequest() {
    }

    /**
     * Constructs a new {@code JsonRequest} with the key-value pair and TTL.
     *
     * @param key        key associated with record
     * @param value      record value
     * @param ttlSeconds time to live in seconds
     */
    public JsonRequest(String key, String value, Long ttlSeconds) {
        this.key = key;
        this.value = value;
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Gets key associated with record.
     *
     * @return key associated with record
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets key associated with record.
     *
     * @param key key associated with record
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets record value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets record value.
     *
     * @param value record value
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets time to live for record.
     *
     * @return time to live in seconds
     */
    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    /**
     * Sets time to live for record.
     *
     * @param ttlSeconds time to live in seconds
     */
    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String toString() {
        return String.format("{\"key\":\"%s\", \"value\":\"%s\", \"ttl\":%s}",
                key != null ? key : "",
                value != null ? value : "",
                ttlSeconds != null ? ttlSeconds.toString() : "null");
    }
}
