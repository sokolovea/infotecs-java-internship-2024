package ru.infotecs.internship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.infotecs.internship.driver.StorageDriver;
import ru.infotecs.internship.driver.StorageDriverException;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@SpringBootApplication
public class InternshipApplication {

    public static void main(String[] args) {
        System.out.println("Internship Application Started");
        SpringApplication.run(InternshipApplication.class, args);
        StorageDriver a = null;
        try {
            a = StorageDriver.connectStorage("127.0.0.1", 8080);
            System.out.println("Connected to Storage!");
//            System.out.println(a.getValue("1"));
            Path dirPath = Paths.get("d:");
            a.dump(dirPath, "dumpik.dat");
            a.load(dirPath, "test.conf");
        } catch (StorageDriverException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
