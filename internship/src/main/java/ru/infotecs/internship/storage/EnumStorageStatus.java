package ru.infotecs.internship.storage;

public enum EnumStorageStatus {
    KEY_EMPTY("Key is empty"),
    KEY_NOT_EXIST("Key does not exist"),
    VALUE_EMPTY("Value is empty"),
    VALUE_GET_OK("Value get ok"),
    VALUE_GET_ERROR("Error while get value"),
    VALUE_SET_OK("Value set ok"),
    VALUE_SET_UPDATE_OK("Value update ok"),
    VALUE_SET_ERROR("Error while set value"),
    VALUE_REMOVE_OK("Value remove ok"),
    VALUE_REMOVE_ERROR("Error while remove value"),
    VALUE_DUMP_OK("Value dump ok"),
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
