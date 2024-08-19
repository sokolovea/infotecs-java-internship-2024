package ru.infotecs.internship.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    /**
     * Key-value database with TTL support.
     */
    private StorageMap storageMap;


    private static ConcurrentHashMap<String, RecordValue> getRawStorage(StorageMap storageMap)
            throws NoSuchFieldException, IllegalAccessException {
        Field storageField = StorageMap.class.getDeclaredField("storage");
        storageField.setAccessible(true);
        return (ConcurrentHashMap<String, RecordValue>) storageField.get(storageMap);
    }

    /**
     * Constructs new storageMap object before each test case.
     */
    @BeforeEach
    public void setUp() {
        storageMap = new StorageMap();
    }

    /**
     * Stops trim process for garbage objects.
     */
    @AfterEach
    public void shutDown() {
        storageMap.stopTrim();
    }

    @Test
    public void putNewValueShouldCreateNewRecord() {
        storageMap.putValue("myKey", "myValue");
        assertEquals("myValue", storageMap.getValue("myKey").getValue());
    }

    @Test
    public void putExistingValueShouldUpdateValueAndTTL() {
        storageMap.putValue("myKey", "myValue", null);
        storageMap.putValue("myKey", "myValue2", 700L);
        RecordValue recordValue = storageMap.getValue("myKey");
        assertEquals("myValue2", recordValue.getValue());
        assertEquals((double) 700L, (double) recordValue.getTtlMs() / 1000, MAX_STORAGE_DELAY);
    }

    @Test
    public void getValueShouldReturnNullForNonExistentKey() {
        assertNull(storageMap.getValue("nonExistentKey"));
    }

    @Test
    public void expiredValueShouldNotBeGot() throws InterruptedException {
        storageMap.putValue("myKey", "myValue", 1L);
        Thread.sleep(1000 + DELTA_TIME_MS);
        assertNull(storageMap.getValue("myKey"));
    }

    @Test
    public void getExistedValueShouldReturnCorrectRecord() {
        storageMap.putValue("myKey", "myValue");
        RecordValue recordValue = storageMap.getValue("myKey");
        assertEquals("myValue", recordValue.getValue());
    }

    @Test
    public void getNotExistedValueShouldReturnNull() {
        storageMap.putValue("myKey", "myValue");
        RecordValue recordValue = storageMap.getValue("myKey2");
        assertNull(recordValue);
    }

    @Test
    public void removeExistedValueShouldBeCorrect() {
        storageMap.putValue("myKey", "myValue", null);
        RecordValue recordValue = storageMap.removeValue("myKey");
        assertEquals("myValue", recordValue.getValue());
    }

    @Test
    public void removeNotExistedValueShouldBeCorrect() {
        assertNull(storageMap.removeValue("myKey"));
    }

    @Test
    public void sameObjectsShouldBeEqual() {
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        StorageMap storageMapSecond = new StorageMap();
        storageMapSecond.putValue("myKey", "myValue", 1L);
        storageMapSecond.putValue("myKey2", "myValue2", 1L);
        assertEquals(storageMap, storageMapSecond);
        storageMapSecond.stopTrim();
    }

    @Test
    public void differObjectsShouldNotBeEqual() {
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        StorageMap storageMapSecond = new StorageMap();
        storageMapSecond.putValue("myKey", "myValue", 1L);
        storageMapSecond.putValue("myKey_OTHER", "myValue_OTHER", 1L);
        assertNotEquals(storageMap, storageMapSecond);
    }

    @Test
    public void serializationAndDeserializationShouldBeCorrect() throws IOException, ClassNotFoundException {
        StorageMap desetializedStorageMap;
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
        if (desetializedStorageMap != null) {
            desetializedStorageMap.stopTrim();
        }
    }

    @Test
    public void startTrimShouldStartCleanupProcess()
            throws NoSuchFieldException, InterruptedException, IllegalAccessException {
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        Thread.sleep(StorageMap.TRIM_DELAY_MS * 2);
        var rawStorage = getRawStorage(storageMap);
        assertEquals(0, rawStorage.size());
    }

    @Test
    public void stopTrimShouldDestroyCleanupProcess()
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        storageMap.stopTrim();
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        Thread.sleep(StorageMap.TRIM_DELAY_MS * 2);
        var rawStorage = getRawStorage(storageMap);
        assertEquals(2, rawStorage.size());
    }
}