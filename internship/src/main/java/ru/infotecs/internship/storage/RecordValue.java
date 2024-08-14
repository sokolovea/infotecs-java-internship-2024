package ru.infotecs.internship.storage;

import java.io.Serializable;

public class RecordValue implements Serializable {
    private String value;
    private int tll;

    public RecordValue(String value, int tll) {
        this.value = value;
        this.tll = tll;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getTll() {
        return tll;
    }

    public void setTll(int tll) {
        this.tll = tll;
    }
}
