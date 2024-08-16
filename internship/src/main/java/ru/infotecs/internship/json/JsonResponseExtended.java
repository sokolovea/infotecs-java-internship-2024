package ru.infotecs.internship.json;

import ru.infotecs.internship.storage.EnumStorageStatus;

/**
 * Extends the {@link JsonResponse} class to include additional data.
 */
public class JsonResponseExtended extends JsonResponse {

    /**
     * Additional data included in the response.
     */
    private String data = null;

    /**
     * Constructs a new {@code JsonResponseExtended} with the given status.
     *
     * @param status the status of the response, indicating the result of user operation.
     */
    public JsonResponseExtended(EnumStorageStatus status) {
        super(status);
    }

    /**
     * Constructs a new {@code JsonResponseExtended} with the given status and additional data.
     *
     * @param status the status of the response, indicating the result of an operation.
     * @param data the additional data to include in the response.
     */
    public JsonResponseExtended(EnumStorageStatus status, String data) {
        super(status);
        this.data = data;
    }

    /**
     * Gets the additional data included in the response.
     *
     * @return the additional data, or {@code null} if no data was provided.
     */
    public String getData() {
        return data;
    }

    /**
     * Sets the additional data to include in the response.
     *
     * @param data the additional data to set.
     */
    public void setData(String data) {
        this.data = data;
    }
}
