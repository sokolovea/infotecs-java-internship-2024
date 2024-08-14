package ru.infotecs.internship.storage;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.Serializable;

public class RecordValue implements Serializable {

    @JsonProperty("value")
    private String value = null;

    @JsonProperty("ttl")
    private Integer ttl = null;

    public RecordValue() {
    }

    public RecordValue(String value, int ttl) {
        this.value = value;
        this.ttl = ttl;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Integer getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }
}
