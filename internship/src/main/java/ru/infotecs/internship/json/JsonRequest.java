package ru.infotecs.internship.json;

import com.fasterxml.jackson.annotation.JsonProperty;

public class JsonRequest {
    @JsonProperty("key")
    private String key = null;

    @JsonProperty("value")
    private String value = null;

    @JsonProperty("ttl")
    private Long ttlSeconds = null;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getTtlSeconds() {
        return ttlSeconds;
    }

    public void setTtlSeconds(Long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }
}
