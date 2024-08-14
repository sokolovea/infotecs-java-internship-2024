package ru.infotecs.internship;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.infotecs.internship.storage.RecordValue;
import ru.infotecs.internship.storage.StorageMap;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;


@RestController
@RequestMapping("/storage")
public class StorageController {

    private static volatile StorageMap storage = new StorageMap();

    @GetMapping("{key}")
    public ResponseEntity<?> getData(@PathVariable String key) {
        RecordValue value = null;
        try {
            value = storage.getValue(key);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Value not exists");
        }
        return ResponseEntity.status(HttpStatus.OK).body(value);
    }

    @PostMapping
    public ResponseEntity<?> setData(@RequestParam String key, @RequestParam String value, @RequestParam(required = false) Integer ttl) {
        if (value == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Value is empty");
        } else {
            try {
                if (ttl == null) {
                    StorageController.storage.putValue(key, value);
                } else {
                    StorageController.storage.putValue(key, value, ttl);
                }
            } catch (NullPointerException e) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error while set data");
            }
            return ResponseEntity.status(HttpStatus.OK).body("OK");
        }
    }

    @GetMapping("/dump")
    public ResponseEntity<?> dumpStorage() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(storage);
            byte[] data = baos.toByteArray();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentLength(data.length);
            headers.setContentDispositionFormData("attachment", "storage.dat");

            return new ResponseEntity<>(data, headers, HttpStatus.OK);
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    @PostMapping("/load")
    public ResponseEntity<?> loadStorage(@RequestParam("file") MultipartFile file) {
        try (ObjectInputStream in = new ObjectInputStream(file.getInputStream())) {
            storage = (StorageMap) in.readObject();
            return ResponseEntity.status(HttpStatus.OK).body("Storage loaded successfully");
        } catch (IOException | ClassNotFoundException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error loading storage");
        }
    }

}
