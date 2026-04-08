package com.gmall.pricinginventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PricingInventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PricingInventoryServiceApplication.class, args);
    }
}
