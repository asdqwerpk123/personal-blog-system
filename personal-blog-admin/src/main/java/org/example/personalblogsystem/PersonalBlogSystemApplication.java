package org.example.personalblogsystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"org.example.personalblogsystem", "org.example.personalblogcommon"})
public class PersonalBlogSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(PersonalBlogSystemApplication.class, args);
    }
}