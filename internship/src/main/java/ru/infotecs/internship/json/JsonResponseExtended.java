package ru.infotecs.internship.json;

import ru.infotecs.internship.storage.EnumStorageStatus;

public class JsonResponseExtended extends JsonResponse {

    private String data = null;

    public JsonResponseExtended(EnumStorageStatus status) {
        super(status);
    }

    public JsonResponseExtended(EnumStorageStatus status, String data) {
        super(status);
        this.data = data;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
