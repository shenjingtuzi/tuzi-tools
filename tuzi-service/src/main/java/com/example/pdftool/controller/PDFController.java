package com.example.pdftool.controller;

import com.alibaba.fastjson.JSON;
import com.example.common.Result;
import com.example.pdftool.entity.PageGenerateVo;
import com.example.pdftool.entity.WatermarkInfo;
import com.example.pdftool.entity.WatermarkParams;
import com.example.pdftool.util.PDFUtils;
import com.example.pdftool.util.PDFWatermarkUtils;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pdf")
@CrossOrigin // 小程序跨域支持
public class PDFController {

    @Value("${upload.path:${user.dir}/uploads}")
    private String uploadPathStr;

    private final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    @PostMapping("/addWatermark")
    public Result<Map<String, String>> addWatermark(
            @RequestParam("pdfFile") MultipartFile pdfFile,
            @RequestParam("watermarkParams") String watermarkParamsStr) {
        try {
            // 1. 解析水印参数
            WatermarkParams params = JSON.parseObject(watermarkParamsStr, WatermarkParams.class);
            // 2. 保存原始PDF
            String pdfFileName = UUID.randomUUID() + ".pdf";
//            File originalPdfFile = uploadPath.resolve(pdfFileName).toFile();
//            if (!originalPdfFile.getParentFile().exists()) {
//                originalPdfFile.getParentFile().mkdirs();
//            }
//            pdfFile.transferTo(originalPdfFile);
            // 3. 处理PDF水印
            String processedPdfName = "watermark_" + pdfFileName;
            File processedPdfFile = uploadPath.resolve(processedPdfName).toFile();

            boolean success;
            if ("image".equals(params.getType())) {
                // 图片水印：直接使用上传的图片文件
                success = PDFWatermarkUtils.addImageWatermark(pdfFile, processedPdfFile, params);
            } else {
                // 文字水印
                success = PDFWatermarkUtils.addTextWatermark(pdfFile, processedPdfFile, params);
            }

            if (!success) {
                return Result.failure("PDF加水印失败");
            }
            // 4. 返回处理后的PDF地址
            String pdfUrl = "/api/file/output/" + processedPdfName;
            Map<String, String> data = new HashMap<>();
            data.put("size", String.valueOf(processedPdfFile.length()));
            data.put("url", pdfUrl);
            data.put("fileName", processedPdfName);
            data.put("fileId", pdfFileName.replace(".pdf", ""));
            data.put("type", "pdf");
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("处理PDF失败：" + e.getMessage());
        }
    }

    @PostMapping("/getWaterMark")
    public Result<List<WatermarkInfo>> getWaterMark(@RequestParam("pdfFile") MultipartFile file) {
        try {
            String pdfFileName = UUID.randomUUID() + ".pdf";
            File originalPdfFile = uploadPath.resolve(pdfFileName).toFile();
            if (!originalPdfFile.getParentFile().exists()) {
                originalPdfFile.getParentFile().mkdirs();
            }
            file.transferTo(originalPdfFile);
            List<WatermarkInfo> watermarkInfos = PDFWatermarkUtils.getWatermark(originalPdfFile.getAbsolutePath());
            return Result.success(watermarkInfos);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("获取PDF水印失败：" + e.getMessage());
        }

    }


    @PostMapping("/removeWaterMark")
    public Result<Map<String, String>> removeWaterMark(@RequestParam("pdfFile") MultipartFile file,
                                                       @RequestParam(name = "watermarkIds", required = false) String watermarkIds,
                                                       @RequestParam(name = "type", defaultValue = "all") String type,
                                                       @RequestParam(name = "targetPages", required = false) List<Integer> pages) {
        try {
            String pdfFileName = UUID.randomUUID() + ".pdf";
//            File originalPdfFile = uploadPath.resolve(pdfFileName).toFile();
//            if (!originalPdfFile.getParentFile().exists()) {
//                originalPdfFile.getParentFile().mkdirs();
//            }
//            file.transferTo(originalPdfFile);
            String processedPdfName = "removewatermark_" + pdfFileName;
            File processedPdfFile = uploadPath.resolve(processedPdfName).toFile();

            boolean success = PDFWatermarkUtils.removeAllWatermarks(file, processedPdfFile.getAbsolutePath(), type, watermarkIds, pages);

            if (!success) {
                return Result.failure("PDF加水印失败");
            }
            // 4. 返回处理后的PDF地址
            String pdfUrl = "/api/file/output/" + processedPdfName;
            Map<String, String> data = new HashMap<>();
            data.put("size", String.valueOf(processedPdfFile.length()));
            data.put("url", pdfUrl);
            data.put("fileName", processedPdfName);
            data.put("fileId", pdfFileName.replace(".pdf", ""));
            data.put("type", "pdf");
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("处理PDF失败：" + e.getMessage());
        }
    }

    /**
     * POST /api/pdf/to-targetType
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
            if (fileName == null || (!fileName.toLowerCase().endsWith(".pdf"))) {
                return Result.failure("仅支持 .pdf 格式");
            }
            if (file.getSize() > 20 * 1024 * 1024) { // 20MB
                return Result.failure("文件大小不能超过 20MB");
            }
            String targetSuffix;
            String fileId = UUID.randomUUID().toString();
//            File pdfFile = uploadPath.resolve(fileId + ".pdf").toFile();
//            file.transferTo(pdfFile);
            if ("png".equalsIgnoreCase(targetType)) {
                targetSuffix = "zip";
            } else {
                targetSuffix = targetType;
            }
            File targetFile = uploadPath.resolve(fileId + "." + targetSuffix).toFile();
            PDFUtils.convert(file, targetFile, targetType);
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

    @PostMapping("/thumbnails")
    public Result<Map<String, Object>> getPdfThumbnails(@RequestParam("pdfFile") MultipartFile pdfFile) {
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("传入的PDF文件不能为空");
        }
        String originalFileName = pdfFile.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("传入的文件不是PDF格式");
        }
        try {
            String fileName = UUID.randomUUID() + ".pdf";
            File saveFile = uploadPath.resolve(fileName).toFile();
            pdfFile.transferTo(saveFile);
            // 生成小程序适配的缩略图字节数组列表
            List<byte[]> thumbnailBytesList = PDFUtils.generateThumbnails(saveFile);

            // 转为Base64字符串列表（前端可直接渲染）
            List<String> base64ThumbnailList = thumbnailBytesList.stream()
                    .map(bytes -> Base64.getEncoder().encodeToString(bytes))
                    .collect(Collectors.toList());
            Map<String, Object> data = new HashMap<>();
            data.put("fileName", fileName);
            data.put("thumbnails", base64ThumbnailList);
            return Result.success(data);
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure("生成PDF缩略图失败：" + e.getMessage());
        }
    }

    @PostMapping("/generate-pdf")
    public Result<Map<String, String>> generatePdf(@RequestBody PageGenerateVo vo) {
        List<String> pageOrder = vo.getOrder();
        Map<String, Integer> rotateMap = vo.getRotateMap();
        List<Map<String, Object>> addedPages = vo.getAddedPages();
        Map<String, Object> originalPdf = vo.getOriginalPdf();
        String originalFileName = (String) originalPdf.get("name");
        File originalPdfFile = uploadPath.resolve(originalFileName).toFile();
        String pdfFileName = UUID.randomUUID() + ".pdf";
        File pdfFile = uploadPath.resolve(pdfFileName).toFile();
        boolean success = PDFUtils.generate(originalPdfFile, pdfFile, pageOrder, rotateMap, addedPages);
        if (!success) {
            return Result.failure("PDF生成失败");
        }
        String pdfUrl = "/api/file/output/" + pdfFileName;
        Map<String, String> data = new HashMap<>();
        data.put("size", String.valueOf(pdfFile.length()));
        data.put("url", pdfUrl);
        data.put("fileName", pdfFileName);
        data.put("fileId", pdfFileName.replace(".pdf", ""));
        data.put("type", "pdf");
        return Result.success(data);
    }

    @PostMapping(value = "/check-encrypt", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> checkEncryptPdf(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.failure("文件为空");
        }
        if (file.getSize() > 20 * 1024 * 1024) { // 20MB
            return Result.failure("文件大小不能超过 20MB");
        }
        String fileId = UUID.randomUUID().toString();
        File pdfFile = uploadPath.resolve(fileId + ".pdf").toFile();
        try {
            file.transferTo(pdfFile);
        } catch (IOException e) {
            return Result.failure("文件上传失败：" + e.getMessage());
        }
        boolean isEncrypted = PDFUtils.isEncrypted(pdfFile);
        Map<String, Object> data = new HashMap<>();
        data.put("isEncrypted", isEncrypted);
        data.put("fileId", fileId);
        return Result.success(data);
    }

    @PostMapping(value = "/encrypt")
    public Result<Map<String, Object>> encryptPdf(@RequestBody Map<String, Object> encryptParams) {
        String userPassword = encryptParams.get("userPassword").toString();
        String ownerPassword = encryptParams.get("ownerPassword").toString();
        String fileId = encryptParams.get("fileId").toString();

        File pdfFile = uploadPath.resolve(fileId + ".pdf").toFile();

        PDFUtils.encryptPdf(pdfFile, pdfFile.getPath(), userPassword, ownerPassword);
        String outputUrl = "/api/file/output/" + fileId + ".pdf";
        String outputName = fileId + ".pdf";
        long fileSize = pdfFile.length();

        Map<String, Object> data = new HashMap<>();
        data.put("size", fileSize);
        data.put("url", outputUrl);
        data.put("fileName", outputName);
        data.put("fileId", fileId);
        data.put("type", "pdf");
        return Result.success(data);
    }

    @PostMapping(value = "/decrypt")
    public Result<Map<String, Object>> decryptPdf(@RequestBody Map<String, Object> encryptParams) {
        String ownerPassword = encryptParams.get("ownerPassword").toString();
        String fileId = encryptParams.get("fileId").toString();

        File encryptedFile = uploadPath.resolve(fileId + ".pdf").toFile();

        PDFUtils.decryptPdf(encryptedFile, encryptedFile.getPath(), ownerPassword);
        String outputUrl = "/api/file/output/" + fileId + ".pdf";
        String outputName = fileId + ".pdf";
        long fileSize = encryptedFile.length();

        Map<String, Object> data = new HashMap<>();
        data.put("size", fileSize);
        data.put("url", outputUrl);
        data.put("fileName", outputName);
        data.put("fileId", fileId);
        data.put("type", "pdf");
        return Result.success(data);
    }
}
