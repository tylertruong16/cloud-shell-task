package com.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CloudShellTaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudShellTaskApplication.class, args);
    }

}
