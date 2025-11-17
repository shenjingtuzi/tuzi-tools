package com.example.livpconverter.controller;

import com.example.livpconverter.service.LivpService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;

@Slf4j
@RestController
@RequestMapping("/api/livp")
public class LivpController {

    @Autowired
    private LivpService livpService;

    /**
     * 上传视频和封面图并转换为LIVP
     */
    @PostMapping("/convert")
    public ResponseEntity<Resource> convertToLivp(
            @RequestParam("video") MultipartFile videoFile,
            @RequestParam("cover") MultipartFile coverImageFile) {
        try {
            if (videoFile.isEmpty() || coverImageFile.isEmpty()) {
                return ResponseEntity.badRequest().body(null);
            }

            log.info("收到转换请求. 视频大小: {}, 封面图大小: {}",
                    videoFile.getSize(), coverImageFile.getSize());

            File livpFile = livpService.convertToLivp(videoFile, coverImageFile);

            Resource resource = new FileSystemResource(livpFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + livpFile.getName() + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("转换失败", e);
            return ResponseEntity.internalServerError().body(null);
        }
    }
}