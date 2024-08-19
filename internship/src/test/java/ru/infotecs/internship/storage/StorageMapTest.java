package ru.infotecs.internship.storage;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for StorageMap class.
 * The tests cover most of the non-trivial methods.
 */
public class StorageMapTest {

    /**
     * Maximal delay of key-value database in seconds.
     * Needs for correct TTL calculating.
     */
    public static final double MAX_STORAGE_DELAY = 0.01;

    /**
     * Delay for waiting for the trim process to remove expired records.
     */
    public static final long DELTA_TIME_MS = 250L;

    @Test
    void putNewValueShouldCreateNewRecord() {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue");
        assertEquals("myValue", storageMap.getValue("myKey").getValue());
    }

    @Test
    void putExistingValueShouldUpdateValueAndTTL() {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue", null);
        storageMap.putValue("myKey", "myValue2", 700L);
        RecordValue recordValue = storageMap.getValue("myKey");
        assertEquals("myValue2", recordValue.getValue());
        assertEquals((double) 700L, (double) recordValue.getTtlMs() / 1000, MAX_STORAGE_DELAY);
    }

    @Test
    void getValueShouldReturnNullForNonExistentKey() {
        StorageMap storageMap = new StorageMap();
        assertNull(storageMap.getValue("nonExistentKey"));
    }

    @Test
    void expiredValueShouldNotBeGot() throws InterruptedException {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue", 1L);
        Thread.sleep(1000 + DELTA_TIME_MS);
        assertNull(storageMap.getValue("myKey"));
    }

    @Test
    void getExistedValueShouldReturnCorrectRecord() {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue");
        RecordValue recordValue = storageMap.getValue("myKey");
        assertEquals("myValue", recordValue.getValue());
    }

    @Test
    void getNotExistedValueShouldReturnNull() {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue");
        RecordValue recordValue = storageMap.getValue("myKey2");
        assertNull(recordValue);
    }

    @Test
    void removeExistedValueShouldBeCorrect() {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue", null);
        RecordValue recordValue = storageMap.removeValue("myKey");
        assertEquals("myValue", recordValue.getValue());
    }

    @Test
    void removeNotExistedValueShouldBeCorrect() {
        StorageMap storageMap = new StorageMap();
        assertNull(storageMap.removeValue("myKey"));
    }

    @Test
    void sameObjectsShouldBeEqual() {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        StorageMap storageMapSecond = new StorageMap();
        storageMapSecond.putValue("myKey", "myValue", 1L);
        storageMapSecond.putValue("myKey2", "myValue2", 1L);
        assertEquals(storageMap, storageMapSecond);
    }

    @Test
    void differObjectsShouldNotBeEqual() {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        StorageMap storageMapSecond = new StorageMap();
        storageMapSecond.putValue("myKey", "myValue", 1L);
        storageMapSecond.putValue("myKey_OTHER", "myValue_OTHER", 1L);
        assertNotEquals(storageMap, storageMapSecond);
    }

    @Test
    void serializationAndDeserializationShouldBeCorrect() throws IOException, ClassNotFoundException {
        StorageMap storageMap = new StorageMap();
        StorageMap desetializedStorageMap = null;
        storageMap.putValue("myKey", "myValue", 5L);
        storageMap.putValue("myKey2", "myValue2", 10L);
        storageMap.stopTrim();
        try (ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
            objectOutStream.writeObject(storageMap);
            try (ByteArrayInputStream byteInStream = new ByteArrayInputStream(byteOutStream.toByteArray());
                 ObjectInputStream objectInStream = new ObjectInputStream(byteInStream)) {
                desetializedStorageMap = (StorageMap) objectInStream.readObject();
            }
        }
        assertEquals(storageMap, desetializedStorageMap);
    }

    @Test
    void startTrimShouldStartCleanupProcess()
            throws NoSuchFieldException, InterruptedException, IllegalAccessException {
        StorageMap storageMap = new StorageMap();
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        Thread.sleep(StorageMap.TRIM_DELAY_MS * 2);
        var rawStorage = getRawStorage(storageMap);
        assertEquals(0, rawStorage.size());
    }

    @Test
    void stopTrimShouldDestroyCleanupProcess()
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        StorageMap storageMap = new StorageMap();
        storageMap.stopTrim();
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        Thread.sleep(StorageMap.TRIM_DELAY_MS * 2);
        var rawStorage = getRawStorage(storageMap);
        assertEquals(2, rawStorage.size());
    }
    
    private static ConcurrentHashMap<String, RecordValue> getRawStorage(StorageMap storageMap)
            throws NoSuchFieldException, IllegalAccessException, InterruptedException {
        Field storageField = StorageMap.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        return (ConcurrentHashMap<String, RecordValue>) storageField.get(storageMap);
    }
}