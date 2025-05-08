package com.katanapay.routing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PaymentRoutingApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentRoutingApplication.class, args);
    }
}