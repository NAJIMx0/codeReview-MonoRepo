package com.najim.generateservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class GenerateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(GenerateServiceApplication.class, args);
    }

}