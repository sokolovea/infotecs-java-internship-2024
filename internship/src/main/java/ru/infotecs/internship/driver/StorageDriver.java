package ru.infotecs.internship.driver;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.couchbase.CouchbaseProperties;
import org.springframework.http.HttpStatus;
import ru.infotecs.internship.json.JsonResponse;
import ru.infotecs.internship.storage.EnumStorageStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;

public class StorageDriver {

    private String serverURL;

    private StorageDriver() {
    }

    public static StorageDriver connectStorage(String host, int port) throws StorageDriverException {
        return connectStorage(host, port, true);
    }

    public static StorageDriver connectStorage(String host, int port,
                                               boolean isConnectionChecked) throws StorageDriverException {
        StorageDriver driver = new StorageDriver();
        HttpURLConnection connection = null;
        try {
            URI uri = new URI("http", null, host, port, null, null, null);
            driver.serverURL = uri.toString();
            if (!isConnectionChecked) {
                return driver;
            }

            //Connection check
            URL testUrl = new URL(driver.serverURL + "/test");
            connection = (HttpURLConnection) testUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(1000);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpStatus.OK.value()) {
                throw new StorageDriverException("Server connection test failed! Response code is not 200!");
            }

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonResponse jsonResponse = mapper.readValue(response.toString(), JsonResponse.class);

            if (jsonResponse.getStatus() != EnumStorageStatus.CONNECTION_TEST_OK) {
                throw new StorageDriverException(
                        String.format("Server connection test failed! JSON status is not %s!",
                                EnumStorageStatus.CONNECTION_TEST_OK));
            }
        } catch (URISyntaxException e) {
            return null;
        } catch (IOException e) {
            if (e instanceof JacksonException) {
                throw new StorageDriverException("Server connection test failed! JSON response is not valid!");
            }
        } finally {
            if (connection != null)
                connection.disconnect();
        }
        return driver;
    }

//    public String getValue(String key) throws IOException {
//        URL url = new URL(connection.getURL() + "/" + key);
//        HttpURLConnection getConnection = (HttpURLConnection) url.openConnection();
//        getConnection.setRequestMethod("GET");
//
//        int responseCode = getConnection.getResponseCode();
//        if (responseCode == HttpURLConnection.HTTP_OK) {
//            BufferedReader in = new BufferedReader(new InputStreamReader(getConnection.getInputStream()));
//            String inputLine;
//            StringBuilder response = new StringBuilder();
//
//            while ((inputLine = in.readLine()) != null) {
//                response.append(inputLine);
//            }
//            in.close();
//            return response.toString();
//        } else {
//            return null; // Или выбросить исключение
//        }
//    }
//
//    public boolean setValue(String key, String value, Long ttl) throws IOException {
//        URL url = connection.getURL();
//        HttpURLConnection postConnection = (HttpURLConnection) url.openConnection();
//        postConnection.setRequestMethod("POST");
//        postConnection.setRequestProperty("Content-Type", "application/json; utf-8");
//        postConnection.setDoOutput(true);
//
//        String jsonInputString = String.format("{\"key\":\"%s\", \"value\":\"%s\", \"ttlSeconds\":%d}", key, value, ttl);
//
//        try (OutputStream os = postConnection.getOutputStream()) {
//            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
//            os.write(input, 0, input.length);
//        }
//
//        int responseCode = postConnection.getResponseCode();
//        return responseCode == HttpURLConnection.HTTP_OK;
//    }
//
//    public boolean removeValue(String key) throws IOException {
//        URL url = new URL(connection.getURL() + "/" + key);
//        HttpURLConnection deleteConnection = (HttpURLConnection) url.openConnection();
//        deleteConnection.setRequestMethod("DELETE");
//
//        int responseCode = deleteConnection.getResponseCode();
//        return responseCode == HttpURLConnection.HTTP_OK;
//    }
//
//    public byte[] dumpStorage() throws IOException {
//        URL url = new URL(connection.getURL() + "/dump");
//        HttpURLConnection dumpConnection = (HttpURLConnection) url.openConnection();
//        dumpConnection.setRequestMethod("GET");
//
//        int responseCode = dumpConnection.getResponseCode();
//        if (responseCode == HttpURLConnection.HTTP_OK) {
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//            try (InputStream inputStream = dumpConnection.getInputStream()) {
//                byte[] buffer = new byte[1024];
//                int bytesRead;
//                while ((bytesRead = inputStream.read(buffer)) != -1) {
//                    byteArrayOutputStream.write(buffer, 0, bytesRead);
//                }
//            }
//            return byteArrayOutputStream.toByteArray();
//        } else {
//            return null; // Или выбросить исключение
//        }
//    }
//
//    public boolean loadStorage(byte[] data) throws IOException {
//        URL url = new URL(connection.getURL() + "/load");
//        HttpURLConnection loadConnection = (HttpURLConnection) url.openConnection();
//        loadConnection.setRequestMethod("PUT");
//        loadConnection.setDoOutput(true);
//        loadConnection.setRequestProperty("Content-Type", "application/octet-stream");
//
//        try (OutputStream os = loadConnection.getOutputStream()) {
//            os.write(data);
//        }
//
//        int responseCode = loadConnection.getResponseCode();
//        return responseCode == HttpURLConnection.HTTP_CREATED;
//    }
//
//    public void disconnect() {
//        if (connection != null) {
//            connection.disconnect();
//        }
//    }
}

