package ru.infotecs.internship.json.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ru.infotecs.internship.storage.StorageException;
import ru.infotecs.internship.json.JsonResponse;

/**
 * Utility class to process JSON
 */
public class JsonProcessor {

    /**
     * Private constructor, use static methods.
     */
    private JsonProcessor() {
    }

    /**
     * Converts JSON to related object.
     *
     * @param response      string representation of JSON server response
     * @param responseClass class of object to be created
     * @param <T>           the type of class (must be JsonResponse or extend it)
     * @return object representation of JSON
     * @throws StorageException if the conversion failed
     */
    public static <T extends JsonResponse> T parseJson(String response,
                                                       Class<T> responseClass) throws StorageException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(response, responseClass);
        } catch (JsonProcessingException e) {
            throw new StorageException("JSON response is not valid!");
        }
    }
}
