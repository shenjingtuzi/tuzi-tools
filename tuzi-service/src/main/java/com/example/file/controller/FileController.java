package com.example.file.controller;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/file")
@CrossOrigin // 小程序跨域支持
public class FileController {

    private final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    /**
     * GET /api/file/output/{fileId}.pdf
     * 下载生成的 PDF 文件
     */
    @GetMapping("/output/{fileId:.+}")
    public ResponseEntity<Resource> downloadOutput(@PathVariable String fileId) throws IOException {
        File file = uploadPath.resolve(fileId).toFile();
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String type = fileId.substring(fileId.indexOf(".") + 1, fileId.length());
        String contentType = "application/" + type;

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        URLEncoder.encode(file.getName(), "UTF-8") + "\"")
                .body(resource);
    }
}
