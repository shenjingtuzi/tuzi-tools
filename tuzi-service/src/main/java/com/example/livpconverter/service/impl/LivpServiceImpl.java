package com.example.livpconverter.service.impl;

import com.example.livpconverter.service.LivpService;
import com.example.livpconverter.util.MediaUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class LivpServiceImpl implements LivpService {

    @Autowired
    private MediaUtils mediaUtils;

    @Value("${app.temp-path}")
    private String tempPath;

    @Value("${app.output-path}")
    private String outputPath;

    @Override
    public File convertToLivp(MultipartFile videoFile, MultipartFile coverImageFile) throws Exception {
        // 1. 初始化目录
        File tempDir = new File(tempPath);
        File outputDir = new File(outputPath);
        if (!tempDir.exists()) tempDir.mkdirs();
        if (!outputDir.exists()) outputDir.mkdirs();

        // 2. 生成唯一ID，这是关联 HEIC 和 MOV 的关键
        String uniqueId = UUID.randomUUID().toString().replace("-", "");
        String baseFileName = "IMG_" + uniqueId;

        // 3. 保存上传的文件到临时目录
        File tempVideoFile = new File(tempDir, baseFileName + "_temp_video" + getFileExtension(videoFile.getOriginalFilename()));
        File tempCoverImageFile = new File(tempDir, baseFileName + "_temp_cover.png");
        videoFile.transferTo(tempVideoFile);
        coverImageFile.transferTo(tempCoverImageFile);

        File heicFile = null;
        File movFile = null;
        try {
            // 4. 转换封面图为 HEIC
            heicFile = new File(tempDir, baseFileName + ".HEIC");
            mediaUtils.convertToHeic(tempCoverImageFile.getAbsolutePath(), heicFile.getAbsolutePath());

            // 5. 为 HEIC 文件添加 Live Photo 元数据
            mediaUtils.addLivePhotoMetaToHeic(heicFile.getAbsolutePath(), uniqueId);

            // 6. 转换视频为 MOV，并添加 Live Photo 元数据
            movFile = new File(tempDir, baseFileName + ".MOV");
            mediaUtils.convertToMovWithLivePhotoMeta(tempVideoFile.getAbsolutePath(), movFile.getAbsolutePath(), uniqueId);

            // 7. 打包为 ZIP (即 LIVP)，确保 HEIC 在 MOV 之前
            File livpFile = new File(outputDir, baseFileName + ".livp");
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(livpFile))) {
                addToZip(zos, heicFile);
                addToZip(zos, movFile);
            }
            log.info("LIVP 文件生成成功: {}", livpFile);

            return livpFile;

        } finally {
            // 8. 清理临时文件
            if (tempVideoFile.exists()) tempVideoFile.delete();
            if (tempCoverImageFile.exists()) tempCoverImageFile.delete();
            if (heicFile != null && heicFile.exists()) heicFile.delete();
            if (movFile != null && movFile.exists()) movFile.delete();
            log.info("临时文件清理完毕。");
        }
    }

    /**
     * 将文件添加到 ZIP 压缩流中
     */
    private void addToZip(ZipOutputStream zos, File file) throws IOException {
        ZipEntry zipEntry = new ZipEntry(file.getName());
        // 关键：使用 DEFLATED 压缩方法
        zipEntry.setMethod(ZipEntry.DEFLATED);
        zos.putNextEntry(zipEntry);

        try (InputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }
        zos.closeEntry();
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return ".bin"; // 默认后缀
        }
        return fileName.substring(fileName.lastIndexOf('.'));
    }
}