package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * LIVP 转换器应用入口
 */
@SpringBootApplication
public class TuZiToolsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TuZiToolsApplication.class, args);
        System.out.println("tuzi tools Service started successfully!");
    }

}