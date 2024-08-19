package ru.infotecs.internship.driver;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import ru.infotecs.internship.storage.StorageException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StorageDriverTest {

    private static final long DEFAULT_TESTING_TTL_SECONDS = 1;

    @LocalServerPort
    private int port;

    private StorageDriver storageDriver;

    @BeforeEach
    public void setUp() throws StorageException {
        storageDriver = StorageDriver.connectStorage("localhost", port);
    }

    @Test
    public void testConnection() {
        Assertions.assertNotNull(storageDriver);
    }

    @Test
    public void testSetNewValueAndGetItShouldBeCorrect() throws StorageException, IOException {
        storageDriver.set("myKey", "myValue", DEFAULT_TESTING_TTL_SECONDS);
        String responseValue = storageDriver.get("myKey");
        Assertions.assertEquals(responseValue, "myValue");
    }

    @Test
    public void testGetNotExistedValueShouldReturnNull() throws StorageException, IOException {
        String responseValue = storageDriver.get("myKey");
        Assertions.assertNull(responseValue);
    }

    @Test
    public void testRemoveExistedValueShouldReturnValue() throws StorageException, IOException {
        storageDriver.set("myKey", "myValue", DEFAULT_TESTING_TTL_SECONDS);
        String responseValue = storageDriver.remove("myKey");
        Assertions.assertEquals(responseValue, "myValue");
    }

    @Test
    public void testRemoveNotExistedValueShouldReturnNull() throws StorageException, IOException {
        String responseValue = storageDriver.remove("myKey");
        Assertions.assertNull(responseValue);
    }

    @Test
    public void testDumpAndLoad() throws StorageException, IOException, InterruptedException {
        storageDriver.set("myKey", "myValue", DEFAULT_TESTING_TTL_SECONDS);
        Path tempFile = Files.createTempFile("storage", ".dat");
        try {
            storageDriver.dump(tempFile.getParent(), tempFile.getFileName().toString());
            storageDriver.load(tempFile.getParent(), tempFile.getFileName().toString());
            Assertions.assertEquals("myValue", storageDriver.get("myKey"));
            Thread.sleep(DEFAULT_TESTING_TTL_SECONDS * 1000L + 100L);
            Assertions.assertNull(storageDriver.get("myKey"));
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

}