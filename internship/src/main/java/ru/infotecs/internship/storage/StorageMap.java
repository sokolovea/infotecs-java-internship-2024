package ru.infotecs.internship.storage;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

public class StorageMap implements Serializable {
    public static final int DEFAULT_TTL_SECONDS = 60;
    private ConcurrentHashMap<String, RecordValue> storage = new ConcurrentHashMap<>();

    public ConcurrentHashMap<String, RecordValue> getStorage() {
        return storage;
    }

    private void setStorage(ConcurrentHashMap<String, RecordValue> storage) {
        this.storage = storage;
    }

    public void putValue(String key, String value) throws NullPointerException {
        RecordValue newRecordValue = new RecordValue(value, DEFAULT_TTL_SECONDS);
        storage.put(key, newRecordValue);
    }

    public void putValue(String key, String value, int ttl) {
        if (!isTtlCorrect(ttl)) {
            ttl = DEFAULT_TTL_SECONDS;
        }
        RecordValue recordValue = new RecordValue(value, ttl);
        storage.put(key, recordValue);
    }

    public RecordValue getValue(String key) throws NullPointerException {
        return storage.get(key);
    }

    public RecordValue removeValue(String key) {
        return storage.remove(key);
    }

    public Boolean isKeyExist(String key) {
        try {
            return storage.containsKey(key);
        }
        catch (NullPointerException e) {
            return false;
        }
    }

    public static Boolean isTtlCorrect(int ttl) {
        return (ttl > 0);
    }

}
