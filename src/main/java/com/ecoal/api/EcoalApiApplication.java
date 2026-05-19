package com.ecoal.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class EcoalApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(EcoalApiApplication.class, args);
    }
}
