package ru.infotecs.internship.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.parameters.*;

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
    @Operation(summary = "Gets a value from the storage", description = "Retrieves a value by key from the storage.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Value retrieved successfully",
                    content = @Content(schema = @Schema(implementation = JsonResponseExtended.class))),
            @ApiResponse(responseCode = "404", description = "Value not found",
                    content = @Content(schema = @Schema(implementation = JsonResponseExtended.class)))
    })
    @GetMapping("/storage/{key}")
    public ResponseEntity<?> getValue(@Parameter(name = "key", description = "The key for the value to get",
            required = true, example = "myKey") @PathVariable String key) {
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
    @Operation(
            summary = "Sets a value in the storage",
            description = "Sets or updates a value in the storage with an optional TTL.",
            requestBody = @RequestBody(
                    description = "Request body containing key, value, and optional TTL",
                    required = true,
                    content = @Content(
                            schema = @Schema(implementation = JsonRequest.class),
                            examples = @ExampleObject(name = "Request Example",
                                    value = "{\"key\":\"myKey\",\"value\":\"myValue\",\"ttl\":10}")
                    )
            )
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Value set or updated successfully",
                    content = @Content(schema = @Schema(implementation = JsonResponse.class),
                            examples = @ExampleObject(name = "Request Example",
                                    value = "{\"status\":\"VALUE_SET_UPDATE_OK\",\"timestamp\":\"...\"}"))),
            @ApiResponse(responseCode = "400", description = "Invalid request",
                    content = @Content(schema = @Schema(implementation = JsonResponse.class),
                            examples = @ExampleObject(name = "Request Example",
                                    value = "{\"status\":\"KEY_EMPTY\",\"timestamp\":\"...\"}")))
    })
    @PostMapping("/storage")
    public ResponseEntity<?> setValue(@org.springframework.web.bind.annotation.RequestBody JsonRequest requestBody) {
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
    @Operation(
            summary = "Removes a value from the storage",
            description = "Removes a value from the storage by key. Returns deleted value if it existed",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Value removed successfully",
                            content = @Content(schema = @Schema(implementation = JsonResponseExtended.class),
                                    examples = @ExampleObject(name = "Request Example",
                                            value = "{\"status\":\"VALUE_REMOVE_OK\",\"timestamp\":\"...\", \"data\":\"myValue\"}"))),
                    @ApiResponse(responseCode = "404", description = "Value not found",
                            content = @Content(schema = @Schema(implementation = JsonResponseExtended.class),
                                    examples = @ExampleObject(name = "Request Example",
                                            value = "{\"status\":\"VALUE_NOT_EXIST\",\"timestamp\":\"...\", \"data\":null}")))
            }
    )
    @DeleteMapping("/storage/{key}")
    public ResponseEntity<?> removeValue(@Parameter(name = "key", description = "The key for the value to remove",
            required = true, example = "myKey") @PathVariable String key) {
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
    @Operation(
            summary = "Dump storage data",
            description = "Download the current storage data as a file.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Storage data dumped successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)),
                    @ApiResponse(responseCode = "500", description = "Error while dumping storage",
                            content = @Content(schema = @Schema(implementation = JsonResponse.class),
                                    examples = @ExampleObject(name = "Request Example",
                                            value = "{\"status\":\"VALUE_DUMP_ERROR\",\"timestamp\":\"...\"}")))
            }
    )
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
    @Operation(
            summary = "Loads storage data",
            description = "Loads storage data from an uploaded file.",
            requestBody = @RequestBody(
                    description = "File containing serialized storage data",
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Storage data loaded successfully",
                            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    examples = @ExampleObject(name = "Request Example",
                                            value = "{\"status\":\"VALUE_LOAD_OK\",\"timestamp\":\"...\"}"))),
                    @ApiResponse(responseCode = "500", description = "Error while loading storage",
                            content = @Content(schema = @Schema(implementation = JsonResponse.class),
                                    examples = @ExampleObject(name = "Request Example",
                                            value = "{\"status\":\"VALUE_LOAD_ERROR\",\"timestamp\":\"...\"}")))
            }
    )
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
    @Operation(
            summary = "Tests server connection",
            description = "Tests that the server is working properly. It is used for driver.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Connection test successful",
                            content = @Content(schema = @Schema(implementation = JsonResponse.class),
                                    examples = @ExampleObject(name = "Success Example",
                                            value = "{\"status\":\"CONNECTION_TEST_OK\",\"timestamp\":\"2024-08-20T12:00:00Z\"}")))
            }
    )
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
