package com.example.wordtool.controller;

import com.example.common.Result;
import com.example.wordtool.util.WordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/word")
@CrossOrigin // 小程序跨域支持
public class WordController {

    @Value("${upload.path:${user.dir}/uploads}")
    private String uploadPathStr;

    private final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    public WordController() throws IOException {
        Files.createDirectories(uploadPath);
    }

    /**
     * POST /api/word/to-targetType
     * 将上传的 .docx 转换为指定格式
     */
    @PostMapping(value = "/to-targetType", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("targetType") String targetType) {
        try {
            // 校验文件
            if (file.isEmpty()) {
                return Result.failure("文件为空");
            }
            if (fileName == null || !fileName.toLowerCase().endsWith(".docx")) {
                return Result.failure("仅支持 .docx 格式");
            }
            if (file.getSize() > 20 * 1024 * 1024) { // 20MB
                return Result.failure("文件大小不能超过 20MB");
            }
            String targetSuffix;
            String fileId = UUID.randomUUID().toString();
            // 保存上传文件
//            File docxFile = uploadPath.resolve(fileId + ".docx").toFile();
//            file.transferTo(docxFile);
            if ("png".equalsIgnoreCase(targetType)) {
                targetSuffix = "zip";
            } else {
                targetSuffix = targetType;
            }
            File targetFile = uploadPath.resolve(fileId + "." + targetSuffix).toFile();
            // 执行转换
            WordUtils.convert(file, targetFile, targetType);

            String outputUrl = "/api/file/output/" + fileId + "." + targetSuffix;
            String outputName = fileName.substring(0, fileName.lastIndexOf('.')) + "." + targetSuffix;
            long fileSize = targetFile.length();

            Map<String, Object> data = new HashMap<>();
            data.put("size", fileSize);
            data.put("url", outputUrl);
            data.put("fileName", outputName);
            data.put("fileId", fileId);
            data.put("type", targetSuffix);
            return Result.success(data);

        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("转换失败：" + e.getMessage());
        }
    }

    /**
     * POST /api/word/compressor
     * 压缩上传的 .docx 文件
     */
    @PostMapping(value = "/compressor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> compressWord(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("compressionLevel") int compressionLevel) {
        try {
            // 1. 校验文件
            if (file.isEmpty()) {
                return Result.failure("文件为空");
            }
            if (fileName == null || (!fileName.toLowerCase().endsWith(".docx") && !fileName.toLowerCase().endsWith(".doc"))) {
                return Result.failure("仅支持 .docx 和 .doc 格式");
            }
            if (file.getSize() > 20 * 1024 * 1024) { // 20MB
                return Result.failure("文件大小不能超过 20MB");
            }
            // 2. 保存上传文件
            String fileId = UUID.randomUUID().toString();
//            File docxFile = uploadPath.resolve(fileId + ".docx").toFile();
//            file.transferTo(docxFile);
            // 3. 创建输出文件
            File compressedFile = uploadPath.resolve(fileId + ".docx").toFile();
            // 4. 执行压缩
            WordUtils.compressWord(file, compressedFile.getPath(), compressionLevel);
            // 5. 返回结果（前端可通过 /api/file/output/{id} 下载）
            String outputUrl = "/api/file/output/" + fileId + ".docx";
            String outputName = fileName.substring(0, fileName.lastIndexOf('.')) + ".docx";
            long fileSize = compressedFile.length();

            Map<String, Object> data = new HashMap<>();
            data.put("size", fileSize);
            data.put("url", outputUrl);
            data.put("fileName", outputName);
            data.put("fileId", fileId);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("压缩失败：" + e.getMessage());
        }

    }

    @PostMapping(value = "/check-encrypt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> checkEncryptWord(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.failure("文件为空");
        }
        if (file.getSize() > 20 * 1024 * 1024) { // 20MB
            return Result.failure("文件大小不能超过 20MB");
        }
        String fileId = UUID.randomUUID().toString();
        File docxFile = uploadPath.resolve(fileId + ".docx").toFile();
        try {
            file.transferTo(docxFile);
        } catch (IOException e) {
            return Result.failure("文件上传失败：" + e.getMessage());
        }
        boolean isEncrypted = WordUtils.isEncrypted(docxFile);
        Map<String, Object> data = new HashMap<>();
        data.put("isEncrypted", isEncrypted);
        data.put("fileId", fileId);
        return Result.success(data);
    }

    @PostMapping(value = "/encrypt")
    public Result<Map<String, Object>> encryptWord(@RequestBody Map<String, Object> encryptParams) {
        String ownerPassword = encryptParams.get("password").toString();
        String fileId = encryptParams.get("fileId").toString();

        File docxFile = uploadPath.resolve(fileId + ".docx").toFile();

        WordUtils.encryptWord(docxFile, docxFile.getPath(), ownerPassword);
        String outputUrl = "/api/file/output/" + fileId + ".docx";
        String outputName = docxFile.getName();
        long fileSize = docxFile.length();

        Map<String, Object> data = new HashMap<>();
        data.put("size", fileSize);
        data.put("url", outputUrl);
        data.put("fileName", outputName);
        data.put("type", "docx");
        data.put("fileId", fileId);
        return Result.success(data);
    }

    @PostMapping(value = "/decrypt")
    public Result<Map<String, Object>> decryptWord(@RequestBody Map<String, Object> encryptParams) {
        String ownerPassword = encryptParams.get("password").toString();
        String fileId = encryptParams.get("fileId").toString();

        File encryptDoc = uploadPath.resolve(fileId + ".docx").toFile();

        WordUtils.decryptWord(encryptDoc, encryptDoc.getPath(), ownerPassword);
        String outputUrl = "/api/file/output/" + fileId + ".docx";
        String outputName = encryptDoc.getName();
        long fileSize = encryptDoc.length();

        Map<String, Object> data = new HashMap<>();
        data.put("size", fileSize);
        data.put("url", outputUrl);
        data.put("fileName", outputName);
        data.put("type", "docx");
        data.put("fileId", fileId);
        return Result.success(data);
    }

}
