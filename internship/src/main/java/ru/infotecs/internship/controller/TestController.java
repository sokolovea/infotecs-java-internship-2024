package ru.infotecs.internship.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.infotecs.internship.json.JsonResponse;
import ru.infotecs.internship.storage.EnumStorageStatus;

@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * Notifies the client that the server is working properly (used in the driver).
     *
     * @return a {@link ResponseEntity} containing the {@link JsonResponse} that
     * contains the result of operation (successful) and timestamp.
     */
    @GetMapping()
    public ResponseEntity<?> getValue() {
        return ResponseEntity.status(HttpStatus.OK).body(
                new JsonResponse(EnumStorageStatus.CONNECTION_TEST_OK));
    }
}
