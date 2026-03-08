package com.apexfintech.checkdeposit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CheckDepositApplication {

    public static void main(String[] args) {
        SpringApplication.run(CheckDepositApplication.class, args);
    }
}
