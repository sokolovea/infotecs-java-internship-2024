package ru.infotecs.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import ru.infotecs.internship.json.JsonRequest;
import ru.infotecs.internship.json.JsonResponse;
import ru.infotecs.internship.json.JsonResponseExtended;
import ru.infotecs.internship.storage.EnumStorageStatus;
import ru.infotecs.internship.storage.RecordValue;
import ru.infotecs.internship.storage.StorageMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

@RestController
@RequestMapping("/storage")
public class StorageController {

    @Autowired
    private volatile StorageMap storage;

    @GetMapping("{key}")
    public ResponseEntity<?> getData(@PathVariable String key) {
        RecordValue value = null;
        value = storage.getValue(key);
        if (value == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    new JsonResponseExtended(EnumStorageStatus.VALUE_NOT_EXIST));
        }
        return ResponseEntity.status(HttpStatus.OK).body(
                new JsonResponseExtended(EnumStorageStatus.VALUE_GET_OK, value.getValue()));
    }

    @PostMapping
    public ResponseEntity<?> setData(@RequestBody JsonRequest requestBody) {
        Boolean isValueAlreadyExists = false;

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
            Long ttlMs = ttl * 1000;
            storage.putValue(key, value, ttlMs);
        }
        EnumStorageStatus okStatus =
                isValueAlreadyExists ? EnumStorageStatus.VALUE_SET_UPDATE_OK : EnumStorageStatus.VALUE_SET_OK;
        return ResponseEntity.status(HttpStatus.OK).body(new JsonResponse(okStatus));
    }

    @GetMapping("/dump")
    public ResponseEntity<?> dumpStorage() {
        try {
            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ObjectOutputStream out = new ObjectOutputStream(byteStream);
            out.writeObject(storage);
            byte[] data = byteStream.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(data.length);
            headers.setContentDispositionFormData("attachment", "storage.dat"); // for browser
            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/load")
    public ResponseEntity<?> loadStorage(@RequestParam("file") MultipartFile file) {
        try (ObjectInputStream in = new ObjectInputStream(file.getInputStream())) {
            storage.stopTrim();
            storage = (StorageMap) in.readObject();
            return ResponseEntity.status(HttpStatus.OK).body(new JsonResponse(EnumStorageStatus.VALUE_LOAD_OK));
        } catch (IOException | ClassNotFoundException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new JsonResponse(EnumStorageStatus.VALUE_LOAD_ERROR));
        }
    }

}
