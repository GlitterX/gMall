package com.gmall.foundation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FoundationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(FoundationServiceApplication.class, args);
    }
}
