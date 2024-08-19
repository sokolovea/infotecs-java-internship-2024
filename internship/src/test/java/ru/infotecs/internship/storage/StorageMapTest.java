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

    /**
     * Constycts new storageMap object before each test case.
     * @throws Exception if problems with starting trim thread
     */
    @BeforeEach
    public void setUp() throws Exception {
        storageMap = new StorageMap();
    }

    /**
     * Stops trim process for garbage objects.
     * @throws Exception if problems with stopping trim thread
     */
    @AfterEach
    public void shutDown() throws Exception {
        storageMap.stopTrim();
    }

    @Test
    void putNewValueShouldCreateNewRecord() {
        storageMap.putValue("myKey", "myValue");
        assertEquals("myValue", storageMap.getValue("myKey").getValue());
    }

    @Test
    void putExistingValueShouldUpdateValueAndTTL() {
        storageMap.putValue("myKey", "myValue", null);
        storageMap.putValue("myKey", "myValue2", 700L);
        RecordValue recordValue = storageMap.getValue("myKey");
        assertEquals("myValue2", recordValue.getValue());
        assertEquals((double) 700L, (double) recordValue.getTtlMs() / 1000, MAX_STORAGE_DELAY);
    }

    @Test
    void getValueShouldReturnNullForNonExistentKey() {
        assertNull(storageMap.getValue("nonExistentKey"));
    }

    @Test
    void expiredValueShouldNotBeGot() throws InterruptedException {
        storageMap.putValue("myKey", "myValue", 1L);
        Thread.sleep(1000 + DELTA_TIME_MS);
        assertNull(storageMap.getValue("myKey"));
    }

    @Test
    void getExistedValueShouldReturnCorrectRecord() {
        storageMap.putValue("myKey", "myValue");
        RecordValue recordValue = storageMap.getValue("myKey");
        assertEquals("myValue", recordValue.getValue());
    }

    @Test
    void getNotExistedValueShouldReturnNull() {
        storageMap.putValue("myKey", "myValue");
        RecordValue recordValue = storageMap.getValue("myKey2");
        assertNull(recordValue);
    }

    @Test
    void removeExistedValueShouldBeCorrect() {
        storageMap.putValue("myKey", "myValue", null);
        RecordValue recordValue = storageMap.removeValue("myKey");
        assertEquals("myValue", recordValue.getValue());
    }

    @Test
    void removeNotExistedValueShouldBeCorrect() {
        assertNull(storageMap.removeValue("myKey"));
    }

    @Test
    void sameObjectsShouldBeEqual() {
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        StorageMap storageMapSecond = new StorageMap();
        storageMapSecond.putValue("myKey", "myValue", 1L);
        storageMapSecond.putValue("myKey2", "myValue2", 1L);
        assertEquals(storageMap, storageMapSecond);
        storageMapSecond.stopTrim();
    }

    @Test
    void differObjectsShouldNotBeEqual() {
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        StorageMap storageMapSecond = new StorageMap();
        storageMapSecond.putValue("myKey", "myValue", 1L);
        storageMapSecond.putValue("myKey_OTHER", "myValue_OTHER", 1L);
        assertNotEquals(storageMap, storageMapSecond);
    }

    @Test
    void serializationAndDeserializationShouldBeCorrect() throws IOException, ClassNotFoundException {
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
        if (desetializedStorageMap != null) {
            desetializedStorageMap.stopTrim();
        }
    }

    @Test
    void startTrimShouldStartCleanupProcess()
            throws NoSuchFieldException, InterruptedException, IllegalAccessException {
        storageMap.putValue("myKey", "myValue", 1L);
        storageMap.putValue("myKey2", "myValue2", 1L);
        Thread.sleep(StorageMap.TRIM_DELAY_MS * 2);
        var rawStorage = getRawStorage(storageMap);
        assertEquals(0, rawStorage.size());
    }

    @Test
    void stopTrimShouldDestroyCleanupProcess()
            throws InterruptedException, NoSuchFieldException, IllegalAccessException {
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