package ru.infotecs.internship.storage;

/**
 * Represents statuses of operations with the storage.
 */
public enum EnumStorageStatus {

    CONNECTION_TEST_OK("Connection test ok!"),
    KEY_EMPTY("Key is empty"),
    INTERNAL_SERVER_ERROR("Internal server error"),
    VALUE_NOT_EXIST("Value does not exist"),
    VALUE_GET_OK("Value get ok"),
    VALUE_SET_OK("Value set ok"),
    VALUE_SET_UPDATE_OK("Value update ok"),
    VALUE_REMOVE_OK("Value remove ok"),
    VALUE_DUMP_ERROR("Error while dump storage"),
    VALUE_LOAD_OK("Value load ok"),
    VALUE_LOAD_ERROR("Error while load storage");

    private final String status;

    /**
     * Constructs an {@code EnumStorageStatus} with the specified status message.
     *
     * @param status the status message associated with this enum constant.
     */
    private EnumStorageStatus(String status) {
        this.status = status;
    }

    /**
     * Gets the status message associated with this enum constant.
     *
     * @return the status message.
     */
    public String getStatus() {
        return status;
    }
}
