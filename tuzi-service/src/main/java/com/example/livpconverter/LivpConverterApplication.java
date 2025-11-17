package com.example.livpconverter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LIVP 转换器应用入口
 */
@SpringBootApplication
public class LivpConverterApplication {

    public static void main(String[] args) {
        SpringApplication.run(LivpConverterApplication.class, args);
        System.out.println("LIVP Converter Service started successfully!");
    }

}