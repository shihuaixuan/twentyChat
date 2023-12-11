package com.twenty.chat.common;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"com.twenty.chat"})
public class ChatCustomApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatCustomApplication.class);
    }
}
