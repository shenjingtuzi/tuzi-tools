package com.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;

@Configuration
@EnableScheduling
public class FileCleanupConfig {

    private final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    /**
     * 每天凌晨 2 点执行清理
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanUp() {
        System.out.println("开始清理临时文件...");
        File dir = uploadPath.toFile();
        if (!dir.exists()) return;

        File[] files = dir.listFiles();
        if (files == null) return;

        long oneHourAgo = Instant.now().minus(java.time.Duration.ofHours(1)).toEpochMilli();

        int deletedCount = 0;
        for (File f : files) {
            if (f.lastModified() < oneHourAgo) {
                if (f.delete()) {
                    deletedCount++;
                }
            }
        }
        System.out.println("清理完成，删除 " + deletedCount + " 个过期文件");
    }
}
