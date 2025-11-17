package com.example.livpconverter.service.impl;

import com.example.livpconverter.service.LivpService;
import com.example.livpconverter.util.FFmpegUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class LivpServiceImpl implements LivpService {

    @Autowired
    private FFmpegUtils ffmpegUtils;

    @Value("${app.upload.temp-path}")
    private String tempPath;

    @Value("${app.upload.output-path}")
    private String outputPath;

    @Override
    public File convertToLivp(MultipartFile videoFile, MultipartFile coverImageFile) throws Exception {
        // 1. 初始化目录
        File tempDir = new File(tempPath);
        File outputDir = new File(outputPath);
        if (!tempDir.exists()) tempDir.mkdirs();
        if (!outputDir.exists()) outputDir.mkdirs();

        // 2. 生成唯一ID，用于命名文件
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String baseFileName = "IMG_" + uniqueId;

        // 3. 保存上传的文件到临时目录
        File tempVideoFile = new File(tempDir, baseFileName + "_temp" + getFileExtension(videoFile.getOriginalFilename()));
        File tempCoverImageFile = new File(tempDir, baseFileName + "_temp.png");
        videoFile.transferTo(tempVideoFile);
        coverImageFile.transferTo(tempCoverImageFile);

        try {
            // 4. 转换视频为 MOV
            File movFile = new File(tempDir, baseFileName + ".MOV");
            ffmpegUtils.convertToMov(tempVideoFile.getAbsolutePath(), movFile.getAbsolutePath());
            log.info("MOV文件生成成功: {}", movFile);

            // 5. 转换封面图为 HEIC
            File heicFile = new File(tempDir, baseFileName + ".HEIC");
            ffmpegUtils.convertToHeic(tempCoverImageFile.getAbsolutePath(), heicFile.getAbsolutePath());
            log.info("HEIC文件生成成功: {}", heicFile);

            // 6. 打包为 ZIP (即 LIVP)
            File livpFile = new File(outputDir, baseFileName + ".livp");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(livpFile))) {
                addToZip(zos, heicFile);
                addToZip(zos, movFile);
            }
            log.info("LIVP文件生成成功: {}", livpFile);

            return livpFile;

        } finally {
            // 7. 清理临时文件（可选，也可以用定时任务清理）
            // tempVideoFile.delete();
            // tempCoverImageFile.delete();
            // movFile.delete();
            // heicFile.delete();
        }
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }

    /**
     * 将文件添加到ZIP流
     */
    private void addToZip(ZipOutputStream zos, File file) throws IOException {
        ZipEntry zipEntry = new ZipEntry(file.getName());
        zos.putNextEntry(zipEntry);

        try (java.io.FileInputStream fis = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }
}