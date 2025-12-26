package com.example.exceltool.controller;

import com.example.common.Result;
import com.example.exceltool.util.ExcelUtils;
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
@RequestMapping("/api/excel")
@CrossOrigin // 小程序跨域支持
public class ExcelController {

    @Value("${upload.path:${user.dir}/uploads}")
    private String uploadPathStr;

    private final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    public ExcelController() throws IOException {
        Files.createDirectories(uploadPath);
    }

    /**
     * POST /api/excel/to-targetType
     * 将上传的 .xlsx 转换为指定格式
     */
    @PostMapping(value = "/to-targetType", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> convert(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("targetType") String targetType) {
        try {
            if (file.isEmpty()) {
                return Result.failure("文件为空");
            }
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                return Result.failure("仅支持 .xlsx 和 .xls 格式");
            }
            if (file.getSize() > 20 * 1024 * 1024) { // 20MB
                return Result.failure("文件大小不能超过 20MB");
            }
            String targetSuffix;
            String fileId = UUID.randomUUID().toString();
//            File excelFile = uploadPath.resolve(fileId + ".xlsx").toFile();
//            file.transferTo(excelFile);
            if ("png".equalsIgnoreCase(targetType)) {
                targetSuffix = "zip";
            } else {
                targetSuffix = targetType;
            }
            File targetFile = uploadPath.resolve(fileId + "." + targetSuffix).toFile();
            ExcelUtils.convert(file, targetFile, targetType);
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
     * POST /api/excel/compressor
     * 压缩上传的 .xlsx 文件
     */
    @PostMapping(value = "/compressor", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> compressExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileName") String fileName,
            @RequestParam("compressionLevel") int compressionLevel) throws Exception {
        try {
            if (file.isEmpty()) {
                return Result.failure("文件为空");
            }
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                return Result.failure("仅支持 .xlsx 和 .xls 格式");
            }
            if (file.getSize() > 20 * 1024 * 1024) { // 20MB
                return Result.failure("文件大小不能超过 20MB");
            }
            String fileId = UUID.randomUUID().toString();
//            File excelFile = uploadPath.resolve(fileId + ".xlsx").toFile();
//            file.transferTo(excelFile);
            File compressedFile = uploadPath.resolve(fileId + ".xlsx").toFile();
            ExcelUtils.compressExcel(file, compressedFile.getPath(), compressionLevel);
            String outputUrl = "/api/file/output/" + fileId + ".xlsx";
            String outputName = fileName.substring(0, fileName.lastIndexOf('.')) + ".xlsx";
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
    public Result<Map<String, Object>> checkEncryptExcel(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.failure("文件为空");
        }
        if (file.getSize() > 20 * 1024 * 1024) { // 20MB
            return Result.failure("文件大小不能超过 20MB");
        }
        String fileId = UUID.randomUUID().toString();
        File excelFile = uploadPath.resolve(fileId + ".xlsx").toFile();
        try {
            file.transferTo(excelFile);
        } catch (IOException e) {
            return Result.failure("文件上传失败：" + e.getMessage());
        }
        boolean isEncrypted = ExcelUtils.isEncrypted(excelFile);
        Map<String, Object> data = new HashMap<>();
        data.put("isEncrypted", isEncrypted);
        data.put("fileId", fileId);
        return Result.success(data);
    }

    @PostMapping(value = "/encrypt")
    public Result<Map<String, Object>> encryptExcel(@RequestBody Map<String, Object> encryptParams) {
        String password = encryptParams.get("password").toString();
        String fileId = encryptParams.get("fileId").toString();
        if (password == null || password.isEmpty()) {
            return Result.failure("密码不能为空");
        }
        File excelFile = uploadPath.resolve(fileId + ".xlsx").toFile();

        ExcelUtils.encryptExcel(excelFile, excelFile.getPath(), password);
        String outputUrl = "/api/file/output/" + fileId + ".xlsx";
        String outputName = fileId + ".xlsx";
        long fileSize = excelFile.length();

        Map<String, Object> data = new HashMap<>();
        data.put("size", fileSize);
        data.put("url", outputUrl);
        data.put("fileName", outputName);
        data.put("fileId", fileId);
        data.put("type", "xlsx");
        return Result.success(data);
    }

    @PostMapping(value = "/decrypt")
    public Result<Map<String, Object>> decryptExcel(@RequestBody Map<String, Object> encryptParams) {
        String password = encryptParams.get("password").toString();
        String fileId = encryptParams.get("fileId").toString();
        if (password == null || password.isEmpty()) {
            return Result.failure("密码不能为空");
        }
//            File excelFile = uploadPath.resolve(fileId + ".xlsx").toFile();
//            file.transferTo(excelFile);
        File encryptedFile = uploadPath.resolve(fileId + ".xlsx").toFile();
        if (!ExcelUtils.isEncrypted(encryptedFile)) {
            return Result.failure("文件未加密！");
        }
        ExcelUtils.decryptExcel(encryptedFile, encryptedFile.getPath(), password);
        String outputUrl = "/api/file/output/" + fileId + ".xlsx";
        String outputName = fileId + ".xlsx";
        long fileSize = encryptedFile.length();

        Map<String, Object> data = new HashMap<>();
        data.put("size", fileSize);
        data.put("url", outputUrl);
        data.put("fileName", outputName);
        data.put("fileId", fileId);
        data.put("type", "xlsx");
        return Result.success(data);
    }
}
