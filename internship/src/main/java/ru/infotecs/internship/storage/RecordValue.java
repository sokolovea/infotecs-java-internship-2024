package ru.infotecs.internship.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class RecordValue implements Serializable {

    @JsonProperty("value")
    private String value = null;

    @JsonProperty("expirationTime")
    private Long expirationTime = null;

    public RecordValue() {
    }

    public RecordValue(String value, long ttlMs) {
        this.value = value;
        setTtlMs(ttlMs);
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Long getExpirationTime() {
        return expirationTime;
    }

    public void setExpirationTime(Long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public Long getTtlMs() {
        if (expirationTime == null) {
            return null;
        }
        return expirationTime - System.currentTimeMillis();
    }

    public void setTtlMs(long ttlMs) {
        this.expirationTime = System.currentTimeMillis() + ttlMs;
    }
}
