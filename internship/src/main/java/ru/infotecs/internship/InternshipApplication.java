package ru.infotecs.internship;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.infotecs.internship.driver.StorageDriver;


@SpringBootApplication
public class InternshipApplication {

    public static void main(String[] args) {
        System.out.println("Internship Application Started");
        SpringApplication.run(InternshipApplication.class, args);
    }

}
