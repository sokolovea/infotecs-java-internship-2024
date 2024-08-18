package ru.infotecs.internship.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.event.annotation.BeforeTestClass;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import ru.infotecs.internship.json.JsonResponseExtended;
import ru.infotecs.internship.storage.EnumStorageStatus;
import ru.infotecs.internship.storage.RecordValue;
import ru.infotecs.internship.storage.StorageMap;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
}