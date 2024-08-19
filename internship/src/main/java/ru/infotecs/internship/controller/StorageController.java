package ru.infotecs.internship.controller;

import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import ru.infotecs.internship.json.JsonRequest;
import ru.infotecs.internship.json.JsonResponse;
import ru.infotecs.internship.json.JsonResponseExtended;
import ru.infotecs.internship.storage.EnumStorageStatus;
import ru.infotecs.internship.storage.RecordValue;
import ru.infotecs.internship.storage.StorageMap;

import java.io.*;

/**
 * REST controller for managing the storage operations.
 * Provides methods to get, set, remove, dump, and load values from the storage.
 */
@RestController
public class StorageController {

    /**
     * The key-value database with TTL
     */
    @Autowired
    private volatile StorageMap storage;

    /**
     * Gets a value from the storage by key.
     *
     * @param key the key of the record to get
     * @return a {@link ResponseEntity} containing the {@link JsonResponseExtended} that
     * contains the record value and the results of operation and timestamp.
     */
    @GetMapping("/storage/{key}")
    public ResponseEntity<?> getValue(@PathVariable String key) {
        RecordValue value;
        value = storage.getValue(key);
        if (value == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new JsonResponseExtended(EnumStorageStatus.VALUE_NOT_EXIST));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                new JsonResponseExtended(EnumStorageStatus.VALUE_GET_OK, value.getValue()));
    }

    /**
     * Sets a value in the storage with the specified key and optional time to live.
     *
     * @param requestBody the request body containing key, value, and optional TTL
     * @return a {@link ResponseEntity} containing the {@link JsonResponse} that
     * contains the results of operation and timestamp.
     */
    @PostMapping("/storage")
    public ResponseEntity<?> setValue(@RequestBody JsonRequest requestBody) {
        boolean isValueAlreadyExists = false;

        String key = requestBody.getKey();
        String value = requestBody.getValue();
        Long ttl = requestBody.getTtlSeconds();
        if (key == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new JsonResponse(EnumStorageStatus.KEY_EMPTY));
        }
        if (value == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new JsonResponse(EnumStorageStatus.VALUE_NOT_EXIST));
        }
        isValueAlreadyExists = storage.isKeyValid(key);
        if (ttl == null) {
            storage.putValue(key, value);
        } else {
            storage.putValue(key, value, ttl);
        }
        EnumStorageStatus okStatus = isValueAlreadyExists ?
                EnumStorageStatus.VALUE_SET_UPDATE_OK : EnumStorageStatus.VALUE_SET_OK;
        return ResponseEntity.status(HttpStatus.OK).body(new JsonResponse(okStatus));
    }

    /**
     * Removes a value from the storage by key.
     *
     * @param key the key of the record to remove
     * @return a {@link ResponseEntity} containing the {@link JsonResponseExtended} that
     * contains the record value (maybe null) and the results of operation and timestamp.
     */
    @DeleteMapping("/storage/{key}")
    public ResponseEntity<?> removeValue(@PathVariable String key) {
        if (!storage.isKeyValid(key)) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new JsonResponseExtended(EnumStorageStatus.VALUE_NOT_EXIST));
        }
        RecordValue value = storage.removeValue(key);
        return ResponseEntity.status(HttpStatus.OK).body(
                new JsonResponseExtended(EnumStorageStatus.VALUE_REMOVE_OK, value.getValue()));
    }

    /**
     * Dumps the current storage data to a file to download.
     *
     * @return a {@link ResponseEntity} containing the storage data as a file attachment or
     * {@link JsonResponse} that contains the error status and timestamp.
     */
    @GetMapping("/dump")
    public ResponseEntity<?> dumpStorage() {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(byteStream)) {
            out.writeObject(storage);
            byte[] data = byteStream.toByteArray();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(data.length);
            headers.setContentDispositionFormData("attachment", "storage.dat"); // for browser
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new JsonResponse(EnumStorageStatus.VALUE_DUMP_ERROR));
        }
    }

    /**
     * Loads storage data from an uploaded file.
     *
     * @param inputStream stream containing serialized storage data
     * @return a {@link ResponseEntity} containing the {@link JsonResponse} that
     * contains the results of operation and timestamp.
     */
    @PutMapping("/load")
    public ResponseEntity<?> loadStorage(InputStream inputStream) {
        storage.stopTrim();
        try (ObjectInputStream in = new ObjectInputStream(inputStream)) {
            storage = (StorageMap) in.readObject();
            return ResponseEntity.status(HttpStatus.CREATED).body(
                    new JsonResponse(EnumStorageStatus.VALUE_LOAD_OK));
        } catch (IOException | ClassNotFoundException e) {
            storage.startTrim();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    new JsonResponse(EnumStorageStatus.VALUE_LOAD_ERROR));
        }
    }

    /**
     * Notifies the client that the server is working properly (used in the driver).
     *
     * @return a {@link ResponseEntity} containing the {@link JsonResponse} that
     * contains the result of operation (successful) and timestamp.
     */
    @GetMapping("/test")
    public ResponseEntity<?> test() {
        return ResponseEntity.status(HttpStatus.OK).body(
                new JsonResponse(EnumStorageStatus.CONNECTION_TEST_OK));
    }

    /**
     * Processes unhandled exceptions.
     *
     * @return a {@link ResponseEntity} containing the {@link JsonResponse} that
     * contains the error code and timestamp.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new JsonResponse(EnumStorageStatus.INTERNAL_SERVER_ERROR));
    }

    /**
     * Stops the trimming process when the application is shutting down.
     */
    @PreDestroy
    private void shutdown() {
        storage.stopTrim();
    }

}
