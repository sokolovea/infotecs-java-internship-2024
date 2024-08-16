package ru.infotecs.internship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.infotecs.internship.driver.StorageDriver;
import ru.infotecs.internship.driver.StorageDriverException;

@SpringBootApplication
public class InternshipApplication {

    public static void main(String[] args) {
        System.out.println("Internship Application Started");
        SpringApplication.run(InternshipApplication.class, args);
        try {
            var a = StorageDriver.connectStorage("127.0.0.1", 8080);
        } catch (StorageDriverException e) {
            throw new RuntimeException(e);
        }
    }

}
