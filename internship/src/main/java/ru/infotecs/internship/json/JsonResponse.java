package ru.infotecs.internship.json;

import ru.infotecs.internship.storage.EnumStorageStatus;

import java.time.LocalDateTime;

public class JsonResponse {

    private EnumStorageStatus status;
    private String timestamp;

    public JsonResponse(EnumStorageStatus status) {
        this.status = status;
        this.timestamp = LocalDateTime.now().toString();
    }

    public EnumStorageStatus getStatus() {
        return status;
    }

    public void setStatus(EnumStorageStatus status) {
        this.status = status;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

}
