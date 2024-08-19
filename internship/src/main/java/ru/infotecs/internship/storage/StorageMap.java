package ru.infotecs.internship.storage;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Storage for simple key-value database with TTL.
 * Implements {@link Externalizable} for custom serialization.
 */
@Component
public class StorageMap implements Externalizable {

    /**
     * Default time to live in milliseconds for records.
     */
    public static final long DEFAULT_TTL_MS = 60_000L;

    /**
     * Max time to live in milliseconds for records (100 years :-)).
     */
    public static final long MAX_TTL_MS = 3153600000000L;

    /**
     * Delay in millisecond for trim process.
     */
    public static final long TRIM_DELAY_MS = 1000L;

    /**
     * Maximum ttl difference between equals values (because of serialization and other processing delay)
     */
    private static final long DELTA_TIME_MS = 250L;

    /**
     * Scheduled executor service for periodic trimming of expired records.
     */
    private transient ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * Simple key-value storage.
     */
    private ConcurrentHashMap<String, RecordValue> storage = new ConcurrentHashMap<>();

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
     * @param ttlSeconds the time to live in seconds
     * @throws NullPointerException if key or value is null
     */
    public void putValue(String key, String value, Long ttlSeconds) throws NullPointerException {
        putValueTtlSeconds(key, value, ttlSeconds);
    }

    /**
     * Adds a value to the storage with the specified time to live in milliseconds.
     *
     * @param key   the key for the record
     * @param value the value to be stored
     * @param ttlMs the time to live in milliseconds
     * @throws NullPointerException if key or value is null
     */
    public void putValueTtlMs(String key, String value, Long ttlMs) throws NullPointerException {
        if (!isTtlCorrect(ttlMs)) {
            ttlMs = DEFAULT_TTL_MS;
        }
        RecordValue recordValue = new RecordValue(value, ttlMs);
        storage.put(key, recordValue);
    }

    /**
     * Adds a value to the storage with the specified time to live in seconds.
     *
     * @param key        the key for the record
     * @param value      the value to be stored
     * @param ttlSeconds the time to live in seconds
     * @throws NullPointerException if key or value is null
     */
    public void putValueTtlSeconds(String key, String value, Long ttlSeconds) throws NullPointerException {
        Long ttlMs = null;
        if (isTtlCorrect(ttlSeconds)) {
            ttlMs = ttlSeconds * 1000;
        }
        putValueTtlMs(key, value, ttlMs);
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
     *
     * @param ttlMs the TTL value in milliseconds to check
     * @return true if the TTL is not null, false otherwise
     */
    private static Boolean isTtlCorrect(Long ttlMs) {
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
        storage = new ConcurrentHashMap<>((ConcurrentHashMap<String, RecordValue>) in.readObject());
        long oldReferencePointTime = in.readLong();
        long referencePointTime = System.currentTimeMillis();
        long deltaTime = referencePointTime - oldReferencePointTime;
        for (RecordValue recordValue : storage.values()) {
            recordValue.setExpirationTime(recordValue.getExpirationTime() + deltaTime);
        }
        scheduler = Executors.newScheduledThreadPool(1);
    }

    /**
     * Starts the trimming process to remove expired records.
     */
    public void startTrim() {
        scheduler.scheduleAtFixedRate(this::trim, TRIM_DELAY_MS, TRIM_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the trimming process by shutting down the scheduler.
     */
    @PreDestroy
    public void stopTrim() {
        scheduler.shutdownNow();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StorageMap that = (StorageMap) o;
        if (storage.size() != that.storage.size()) {
            return false;
        }
        for (String key : storage.keySet()) {
            if (!that.storage.containsKey(key)) {
                return false;
            }
            RecordValue firstRecord = storage.get(key);
            RecordValue secondRecord = that.storage.get(key);

            if (firstRecord == null && secondRecord != null || firstRecord != null && secondRecord == null) {
                return false;
            }

            if (firstRecord != null) {
                if (Math.abs(firstRecord.getTtlMs() - secondRecord.getTtlMs()) > DELTA_TIME_MS ||
                        !Objects.equals(firstRecord.getValue(), secondRecord.getValue())) {
                    return false;
                }
            }
        }
        return true;
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
        System.out.println("Trimmed! " + storage.size());
    }
}
