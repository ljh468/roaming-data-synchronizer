package com.roaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;

@SpringBootApplication
@EnableBatchProcessing
public class RoamingDataSynchronizerApplication {
    public static void main(String[] args) {
        SpringApplication.run(RoamingDataSynchronizerApplication.class, args);
    }
}