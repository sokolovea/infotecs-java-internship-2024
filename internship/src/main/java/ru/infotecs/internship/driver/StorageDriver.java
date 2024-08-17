package ru.infotecs.internship.driver;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import ru.infotecs.internship.json.JsonResponse;
import ru.infotecs.internship.json.JsonResponseExtended;
import ru.infotecs.internship.storage.EnumStorageStatus;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Driver for developed key-value database.
 */
public class StorageDriver {

    /**
     * Default inputStream
     */
    public static int DEFAULT_TIMEOUT_MS = 1000;

    /**
     * Text representation of server root URL
     */
    private String serverURL;

    /**
     * Representation of server root URL
     */
    private int timeoutMs;

    /**
     * Private constructor. Use factory method connectStorage.
     */
    private StorageDriver() {
    }

    /**
     * Factory method for creating new instance of StorageDriver class.
     * Also checks connection with server, if failed, then throws StorageDriverException.
     *
     * @param host – server address
     * @param port – server port
     * @return new instance of StorageDriver class if connection check has passed
     * @throws StorageDriverException if connection check failed
     */
    public static StorageDriver connectStorage(String host, int port) throws StorageDriverException {
        return connectStorage(host, port, true, DEFAULT_TIMEOUT_MS);
    }

    /**
     * More configurable factory method for creating new instance of StorageDriver class.
     * @param host – server address
     * @param port – server port
     * @param isConnectionChecked – needing for checking connection with server in method
     * @param timeoutMs – timeout for operations with server. If 0 then infinite timeout
     * @return new instance of StorageDriver class
     * @throws StorageDriverException if connection check failed
     */
    public static StorageDriver connectStorage(String host, int port, boolean isConnectionChecked,
                                               int timeoutMs) throws StorageDriverException {
        StorageDriver driver = new StorageDriver();
        HttpURLConnection connection = null;
        driver.timeoutMs = timeoutMs > 0 ? timeoutMs : DEFAULT_TIMEOUT_MS;
        try {
            URI uri = new URI("http", null, host, port, null, null, null);
            driver.serverURL = uri.toString();

            if (!isConnectionChecked) {
                return driver;
            }

            //Connection check block
            URL testUrl = new URL(driver.serverURL + "/test");
            connection = (HttpURLConnection) testUrl.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(timeoutMs);

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpStatus.OK.value()) {
                throw new StorageDriverException("Server connection test failed! Response code is not 200!");
            }

            String response = getResponse(connection);
            JsonResponse jsonResponse = parseJson(response, JsonResponse.class);

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

    /**
     * Gets String representation of server response
     *
     * @param connection – connection with server
     * @return string representation of server response
     * @throws IOException if response process failed
     */
    private static String getResponse(HttpURLConnection connection) throws IOException {
        StringBuilder response = new StringBuilder();
        InputStream inputStream = null;
        if (connection.getResponseCode() >= HttpStatus.BAD_REQUEST.value()) {
            //all error codes
            inputStream = connection.getErrorStream();
        } else {
            inputStream = connection.getInputStream();
        }
        try (BufferedReader in = new BufferedReader(new InputStreamReader(inputStream))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        return response.toString();
    }

    /**
     * Converts JSON to related object.
     * @param response – string representation of JSON server response
     * @param responseClass – class of object to be created
     * @return object representation of JSON
     * @param <T> – the type of class (must be JsonResponse or extend it)
     * @throws IOException if the conversion failed
     */
    private static <T extends JsonResponse> T parseJson(String response, Class<T> responseClass) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response, responseClass);
    }

    /**
     * Gets value by key from the database.
     *
     * @param key – key for database
     * @return value by key
     * @throws IOException if problems with connection occurs
     * @throws StorageDriverException if server sends an incorrect respons.
     */
    public String get(String key) throws IOException, StorageDriverException {
        URL url = new URL(serverURL + "/storage/" + key);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setReadTimeout(1000);

        try {
            String response = getResponse(connection);
            JsonResponseExtended jsonResponseExtended = parseJson(response, JsonResponseExtended.class);
            return jsonResponseExtended.getData();
        } catch (JacksonException e) {
            throw new StorageDriverException("JSON response is not valid!");
        }
    }

    /**
     * Sets value by key for database.
     *
     * @param key – 
     * @param value –
     * @param ttl –
     * @return
     * @throws IOException
     */
    public boolean set(String key, String value, Long ttl) throws IOException {
        URL url = new URL(serverURL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setDoOutput(true);

        String jsonRequest = String.format("{\"key\":\"%s\", \"value\":\"%s\", \"ttlSeconds\":%d}", key, value, ttl);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        String response = getResponse(connection);
        JsonResponse jsonResponse = parseJson(response, JsonResponse.class);

        return jsonResponse.getStatus() == EnumStorageStatus.VALUE_SET_OK
                || jsonResponse.getStatus() == EnumStorageStatus.VALUE_SET_UPDATE_OK;
    }

    public int getTimeoutMs() {
        return timeoutMs;
    }

    public void setTimeoutMs(int timeoutMs) {
        this.timeoutMs = timeoutMs;
    }

    public boolean remove(String key) throws IOException {
        URL url = new URL(serverURL + "/storage/" + key);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("DELETE");

        String response = getResponse(connection);
        JsonResponse jsonResponse = parseJson(response, JsonResponse.class);

        return jsonResponse.getStatus() == EnumStorageStatus.VALUE_REMOVE_OK;
    }

    public void dump(Path dirPath, String fileName) throws IOException {
        URL url = new URL(serverURL + "/dump");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode(); //no JSON
        if (responseCode == HttpURLConnection.HTTP_OK) {
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                File file = new File(dirPath.toString(), fileName);

                if (!file.exists()) {
                    file.createNewFile();
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byteArrayOutputStream.writeTo(fos);
                }
            }
        }

    }

    public boolean load(Path dirPath, String fileName) throws IOException {
        URL url = new URL(serverURL + "/load");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/octet-stream");

        File file = new File(dirPath.toString(), fileName);
        if (!file.canRead()) {
            throw new IOException("Cannot read file: " + file.getAbsoluteFile());
        }

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = connection.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;


            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);  // Пишем данные напрямую в OutputStream
            }

            os.flush();  // Убедимся, что все данные отправлены на сервер
        }

        // Получаем и обрабатываем ответ сервера
        String response = getResponse(connection);
        JsonResponse jsonResponse = parseJson(response, JsonResponse.class);

        return jsonResponse.getStatus() == EnumStorageStatus.VALUE_LOAD_OK;
    }
//
//    public void disconnect() {
//        if (connection != null) {
//            connection.disconnect();
//        }
//    }
}

