package com.gmall.marketingcontent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class MarketingContentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketingContentServiceApplication.class, args);
    }
}
