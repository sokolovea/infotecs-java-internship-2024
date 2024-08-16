package ru.infotecs.internship.storage;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Storage for simple key-value database with TTL.
 * Implements {@link Externalizable} for custom serialization.
 */
@Component()
public class StorageMap implements Externalizable {

    /** Default time to live in milliseconds for records. */
    public static final Long DEFAULT_TTL_MS = 60_000L;

    /** Max time to live in milliseconds for records (100 years :-)). */
    public static final Long MAX_TTL_MS = 3153600000000L;

    /** Simple key-value storage. */
    private ConcurrentHashMap<String, RecordValue> storage = new ConcurrentHashMap<>();

    /** Scheduled executor service for periodic trimming of expired records. */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Default constructor that starts the trimming task.
     */
    public StorageMap() {
        startTrim();
    }

    /**
     * Adds a value to the storage with the default TTL.
     *
     * @param key   the key for the record
     * @param value the value to be stored
     * @throws NullPointerException if key or value is null
     */
    public void putValue(String key, String value) throws NullPointerException {
        RecordValue newRecordValue = new RecordValue(value, DEFAULT_TTL_MS);
        storage.put(key, newRecordValue);
    }

    /**
     * Adds a value to the storage with the specified time to live in seconds.
     *
     * @param key        the key for the record
     * @param value      the value to be stored
     * @param tllSeconds the time to live in seconds
     * @throws NullPointerException if key or value is null
     */
    public void putValue(String key, String value, Long tllSeconds) throws NullPointerException {
        putValueTtlSeconds(key, value, tllSeconds);
    }

    /**
     * Adds a value to the storage with the specified time to live in milliseconds.
     *
     * @param key   the key for the record
     * @param value the value to be stored
     * @param tllMs the time to live in milliseconds
     * @throws NullPointerException if key or value is null
     */
    public void putValueTtlMs(String key, String value, Long tllMs) throws NullPointerException {
        if (!isTtlCorrect(tllMs)) {
            tllMs = DEFAULT_TTL_MS;
        }
        RecordValue recordValue = new RecordValue(value, tllMs);
        storage.put(key, recordValue);
    }

    /**
     * Adds a value to the storage with the specified time to live in seconds.
     *
     * @param key        the key for the record
     * @param value      the value to be stored
     * @param tllSeconds the time to live in seconds
     * @throws NullPointerException if key or value is null
     */
    public void putValueTtlSeconds(String key, String value, Long tllSeconds) throws NullPointerException {
        putValueTtlMs(key, value, tllSeconds);
    }

    /**
     * Gets a value from the storage.
     *
     * @param key the key for the record
     * @return the record value or null if the key is not valid or does not exist
     */
    public RecordValue getValue(String key) {
        if (!isKeyValid(key)) {
            return null;
        }
        return storage.get(key);
    }

    /**
     * Removes a value from the storage.
     *
     * @param key the key for the record
     * @return the removed record value or null if the key does not exist
     */
    public RecordValue removeValue(String key) {
        return storage.remove(key);
    }

    /**
     * Checks if a key de-facto exists in the storage (but the key may not be valid).
     *
     * @param key the key to check
     * @return true if the key exists, false otherwise
     */
    private boolean isKeyExist(String key) {
        try {
            return storage.containsKey(key);
        } catch (NullPointerException e) {
            return false;
        }
    }

    /**
     * Checks if a key is valid.
     *
     * @param key the key to check
     * @return true if the key is valid, false otherwise
     */
    public boolean isKeyValid(String key) {
        return isKeyExist(key) && (storage.get(key).getExpirationTime() > System.currentTimeMillis());
    }

    /**
     * Checks if the provided TTL value in milliseconds is correct.
     * It was decided not to fix ttlMs less than 0 on DEFAULT_TTL_MS automatically,
     * "correct" value means not null and less than MAX_TTL_MS
     * @param ttlMs the TTL value in milliseconds to check
     * @return true if the TTL is not null, false otherwise
     */
    public static Boolean isTtlCorrect(Long ttlMs) {
        return (ttlMs != null) && (ttlMs <= MAX_TTL_MS);
    }

    /**
     * Serializes the state of this object.
     *
     * @param out the output stream to write to
     * @throws IOException if an I/O error occurs
     */
    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(storage);
        out.writeLong(System.currentTimeMillis());
    }

    /**
     * Deserializes the state of this object.
     *
     * @param in the input stream to read from
     * @throws IOException            if an I/O error occurs
     * @throws ClassNotFoundException if a class is not found during deserialization
     */
    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        storage = (ConcurrentHashMap<String, RecordValue>) in.readObject();
        long oldReferencePointTime = in.readLong();
        long referencePointTime = System.currentTimeMillis();
        long deltaTime = referencePointTime - oldReferencePointTime;
        for (RecordValue recordValue : storage.values()) {
            recordValue.setExpirationTime(recordValue.getExpirationTime() + deltaTime);
        }
    }

    /**
     * Starts the trimming process to remove expired records.
     */
    public void startTrim() {
        scheduler.scheduleAtFixedRate(this::trim, 2, 2, TimeUnit.SECONDS);
    }

    /**
     * Stops the trimming process by shutting down the scheduler.
     */
    @PreDestroy
    public void stopTrim() {
        scheduler.shutdownNow();
    }

    /**
     * Trims expired records from the storage.
     */
    private void trim() {
        long currentTime = System.currentTimeMillis();
        for (Map.Entry<String, RecordValue> entry : storage.entrySet()) {
            storage.computeIfPresent(entry.getKey(), (keyInternal, valueInternal) -> {
                if (valueInternal.getExpirationTime() == null || valueInternal.getExpirationTime() < currentTime) {
                    return null;
                }
                return valueInternal;
            });
        }
    }
}
