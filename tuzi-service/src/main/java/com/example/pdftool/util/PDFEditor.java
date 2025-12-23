package com.example.pdftool.util;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.devices.JpegDevice;
import com.aspose.pdf.devices.Resolution;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF缩略图生成工具类（适配微信小程序回显，固定最优参数）
 */
public class PDFEditor {

    // ========== 固定微信小程序适配参数（无需修改，最优配置） ==========
    // 缩略图宽高：适配小程序列表/预览展示（宽200px，高280px，比例接近PDF原比例）
    private static final int THUMBNAIL_WIDTH = 200;
    private static final int THUMBNAIL_HEIGHT = 280;
    // 分辨率：150dpi（兼顾清晰度和文件大小，小程序加载不卡顿）
    private static final int THUMBNAIL_RESOLUTION = 150;
    // JPG质量：80（平衡画质和体积，单张图片约50-100KB）
    private static final int THUMBNAIL_QUALITY = 80;

    /**
     * 生成PDF所有页面的缩略图（适配微信小程序回显，固定参数）
     * @param pdfFile 前端传入的PDF文件（MultipartFile）
     * @return 所有页面缩略图的字节数组列表（按页面顺序排列）
     * @throws IOException 流操作/PDF处理异常
     */
    public static List<byte[]> generateAllPageThumbnailsForMiniProgram(MultipartFile pdfFile) throws IOException {
        // 1. 参数校验
        if (pdfFile == null || pdfFile.isEmpty()) {
            throw new IllegalArgumentException("传入的PDF文件不能为空");
        }
        String originalFileName = pdfFile.getOriginalFilename();
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("传入的文件不是PDF格式");
        }

        // 存储所有页面缩略图字节数组
        List<byte[]> thumbnailList = new ArrayList<>();
        Document pdfDocument = null;

        // 2. 流处理PDF，不落地本地文件
        try (ByteArrayInputStream pdfInputStream = new ByteArrayInputStream(pdfFile.getBytes())) {
            // 加载PDF文档
            pdfDocument = new Document(pdfInputStream);
            int totalPages = pdfDocument.getPages().size();
            if (totalPages == 0) {
                throw new IOException("该PDF文件没有有效页面");
            }

            // 3. 固定配置缩略图生成器
            Resolution res = new Resolution(THUMBNAIL_RESOLUTION);
            JpegDevice jpegDevice = new JpegDevice(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, res, THUMBNAIL_QUALITY);

            // 4. 遍历所有页面生成缩略图
            for (int pageIndex = 1; pageIndex <= totalPages; pageIndex++) {
                try (ByteArrayOutputStream imageBaos = new ByteArrayOutputStream()) {
                    Page currentPage = pdfDocument.getPages().get_Item(pageIndex);
                    jpegDevice.process(currentPage, imageBaos);
                    // 将缩略图字节数组加入结果列表
                    thumbnailList.add(imageBaos.toByteArray());
                }
            }

        } catch (Exception e) {
            throw new IOException("PDF缩略图生成失败（适配小程序）：" + e.getMessage(), e);
        } finally {
            // 释放PDF文档资源
            if (pdfDocument != null) {
                pdfDocument.close();
            }
        }

        return thumbnailList;
    }
}