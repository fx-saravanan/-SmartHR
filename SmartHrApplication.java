package com.smarthr;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SmartHrApplication {
    public static void main(String[] args) {
        SpringApplication.run(SmartHrApplication.class, args);
    }
}
