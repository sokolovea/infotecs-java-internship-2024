package ru.infotecs.internship.storage;

public enum EnumStorageStatus {
    KEY_EMPTY("Key is empty"),
    VALUE_NOT_EXIST("Value does not exist"),
    VALUE_GET_OK("Value get ok"),
    VALUE_SET_OK("Value set ok"),
    VALUE_SET_UPDATE_OK("Value update ok"),
    VALUE_REMOVE_OK("Value remove ok"),
    VALUE_DUMP_ERROR("Error while dump storage"),
    VALUE_LOAD_OK("Value load ok"),
    VALUE_LOAD_ERROR("Error while load storage");

    private final String status;

    private EnumStorageStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
