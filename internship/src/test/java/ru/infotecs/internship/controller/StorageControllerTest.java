package ru.infotecs.internship.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import ru.infotecs.internship.json.JsonRequest;
import ru.infotecs.internship.storage.EnumStorageStatus;
import ru.infotecs.internship.storage.RecordValue;
import ru.infotecs.internship.storage.StorageMap;

import java.io.*;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests for StorageController class.
 * The tests cover most of the methods.
 */
@WebMvcTest(StorageController.class)
class StorageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StorageMap storageMap;

    @Test
    void getExistingValueShouldBeCorrect() throws Exception {
        String key = "key";
        String value = "value";
        RecordValue recordValue = new RecordValue(value, 1000L);
        when(storageMap.getValue(key)).thenReturn(recordValue);

        mockMvc.perform(get("/storage/{key}", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EnumStorageStatus.VALUE_GET_OK.name()))
                .andExpect(jsonPath("$.data").value(value));
    }

    @Test
    void getNotExistingValueShouldBeCorrect() throws Exception {
        String key = "key";
        when(storageMap.getValue(key)).thenReturn(null);

        mockMvc.perform(get("/storage/{key}", key))
                .andExpect(status().is4xxClientError())
                .andExpect(jsonPath("$.status").value(EnumStorageStatus.VALUE_NOT_EXIST.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void setNotExistingValueShouldCreateNewRecord() throws Exception {
        String key = "key";
        String value = "value";

        RecordValue recordValue = new RecordValue(value, 1000L);
        when(storageMap.isKeyValid(key)).thenReturn(false);
        when(storageMap.getValue(key)).thenReturn(recordValue);

        JsonRequest jsonRequest = new JsonRequest();
        jsonRequest.setKey(key);
        jsonRequest.setValue(value);

        mockMvc.perform(post("/storage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(EnumStorageStatus.VALUE_SET_OK.name()));
    }

    @Test
    void setExistingValueShouldUpdateRecord() throws Exception {
        String key = "key";
        String value = "value";

        RecordValue recordValue = new RecordValue(value, 1L);
        when(storageMap.isKeyValid(key)).thenReturn(true);
        when(storageMap.getValue(key)).thenReturn(recordValue);

        JsonRequest jsonRequest = new JsonRequest();
        jsonRequest.setKey(key);
        jsonRequest.setValue(value);
        
        mockMvc.perform(post("/storage")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonRequest.toString()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(EnumStorageStatus.VALUE_SET_UPDATE_OK.name()));
    }

    @Test
    void removeExistingValueShouldReturnValue() throws Exception {
        String key = "key";
        String value = "value";

        RecordValue recordValue = new RecordValue(value, 1L);
        when(storageMap.isKeyValid(key)).thenReturn(true);
        when(storageMap.removeValue(key)).thenReturn(recordValue);

        mockMvc.perform(delete("/storage/{key}", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EnumStorageStatus.VALUE_REMOVE_OK.name()))
                .andExpect(jsonPath("$.data").value(value));
    }

    @Test
    void removeNotExistingValueShouldReturnNull() throws Exception {
        String key = "key";
        String value = "value";

        RecordValue recordValue = new RecordValue(value, 1L);
        when(storageMap.isKeyValid(key)).thenReturn(false);
        when(storageMap.removeValue(key)).thenReturn(null);

        mockMvc.perform(delete("/storage/{key}", key))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EnumStorageStatus.VALUE_NOT_EXIST.name()))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void dumpStorageShouldReturnCorrectResponse() throws Exception {
        String fileName = "storage.dat";

        mockMvc.perform(get("/dump"))
                .andExpect(status().isOk())  // Ожидаем статус 200 OK
                .andExpect(content().contentType(MediaType.APPLICATION_OCTET_STREAM))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "form-data; name=\"attachment\"; filename=\"" + fileName + "\""));
    }

    @Test
    void loadStorageShouldReturnCorrectResponse() throws Exception {
        try (ByteArrayOutputStream byteOutStream = new ByteArrayOutputStream();
             ObjectOutputStream objectOutStream = new ObjectOutputStream(byteOutStream)) {
            objectOutStream.writeObject(new StorageMap());

            mockMvc.perform(put("/load")
                            .content(byteOutStream.toByteArray())
                            .contentType(MediaType.APPLICATION_OCTET_STREAM))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.status").value(EnumStorageStatus.VALUE_LOAD_OK.name()));
        }
    }

}