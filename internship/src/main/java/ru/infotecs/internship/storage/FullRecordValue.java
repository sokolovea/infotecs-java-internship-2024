package ru.infotecs.internship.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FullRecordValue extends RecordValue {

    @JsonProperty("key")
    private String key;

    public FullRecordValue() {
        super();
    }

    public FullRecordValue(String value, int ttl, String key) {
        super(value, ttl);
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }
}
