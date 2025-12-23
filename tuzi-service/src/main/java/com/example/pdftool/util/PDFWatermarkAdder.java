package com.example.pdftool.util;

import com.aspose.pdf.Artifact;
import com.aspose.pdf.Document;
import com.aspose.pdf.FontRepository;
import com.aspose.pdf.PageInfo;
import com.aspose.pdf.WatermarkArtifact;
import com.aspose.pdf.facades.EncodingType;
import com.aspose.pdf.facades.FormattedText;
import com.example.pdftool.entity.WatermarkParams;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Log4j2
@Component
public class PDFWatermarkAdder {


    /**
     * 添加文字水印
     */
//    public boolean addTextWatermark2(File originalPdf, File processedPdf, WatermarkParams params) {
//        try (Document pdfDoc = new com.aspose.pdf.Document(originalPdf.getAbsolutePath())) {
//            List<Integer> targetPages = getTargetPages(pdfDoc.getPages().size(), params);
//            // 1. 创建文字水印
//            com.aspose.pdf.TextStamp textStamp = new com.aspose.pdf.TextStamp(params.getContent());
//            // 2. 设置字体样式
//            textStamp.getTextState().setFontSize(params.getFontSize());
//            textStamp.getTextState().setForegroundColor(getColorFromHex(params.getColor()));
//            textStamp.setOpacity(params.getOpacity());
//            textStamp.setRotateAngle(params.getRotate());
//            // 3. 设置位置（坐标转换：PDF的原点在左下角，需适配）
//            textStamp.setXIndent(params.getX());
//            textStamp.setYIndent(pdfDoc.getPages().get_Item(1).getPageInfo().getHeight() - params.getY());
//            // 4. 应用到指定页面
//            for (int pageNum : targetPages) {
//                pdfDoc.getPages().get_Item(pageNum).addStamp(textStamp);
//                // 添加Stamp不能自动识别为watermark，需要手动设置
//                ArtifactCollection artifacts = pdfDoc.getPages().get_Item(pageNum).getArtifacts();
//                Artifact item = artifacts.get_Item(artifacts.size());
//                item.beginUpdates();
//                item.setSubtype(Artifact.ArtifactSubtype.Watermark);
//                item.saveUpdates();
//            }
//            pdfDoc.save(processedPdf.getAbsolutePath());
//            return true;
//        } catch (Exception e) {
//            log.error("添加文字水印失败", e);
//            return false;
//        }
//    }

//    /**
//     * 添加图片水印
//     */
//
//    public boolean addImageWatermark2(File originalPdf, File processedPdf, WatermarkParams params) {
//        try (com.aspose.pdf.Document pdfDoc = new com.aspose.pdf.Document(originalPdf.getAbsolutePath())) {
//            byte[] imageBytes = Base64.getDecoder().decode(params.getImageBase64());
//            InputStream imageStream = new ByteArrayInputStream(imageBytes);
//            // 1. 获取需要处理的页面
//            List<Integer> targetPages = getTargetPages(pdfDoc.getPages().size(), params);
//            // 2. 创建图片水印（从MultipartFile读取）
//            com.aspose.pdf.ImageStamp imageStamp = new com.aspose.pdf.ImageStamp(imageStream);
//            // 3. 设置图片样式
//            imageStamp.setWidth(params.getWidth());
//            imageStamp.setHeight(params.getHeight());
//            imageStamp.setOpacity(params.getOpacity());
//            imageStamp.setRotateAngle(params.getRotate());
//            // 4. 设置位置（适配PDF坐标）
//            imageStamp.setXIndent(params.getXRatio());
//            imageStamp.setYIndent(pdfDoc.getPages().get_Item(1).getPageInfo().getHeight() - params.getYRatio());
//            // 5. 应用到指定页面
//            for (int pageNum : targetPages) {
//                pdfDoc.getPages().get_Item(pageNum).addStamp(imageStamp);
//                // 添加Stamp不能自动识别为watermark，需要手动设置
//                ArtifactCollection artifacts = pdfDoc.getPages().get_Item(pageNum).getArtifacts();
//                Artifact item = artifacts.get_Item(artifacts.size());
//                item.beginUpdates();
//                item.setSubtype(Artifact.ArtifactSubtype.Watermark);
//                item.saveUpdates();
//                log.info("添加图片水印到artifacts：{}，type：{}", artifacts.size(), pdfDoc.getPages().get_Item(pageNum).getArtifacts().get_Item(artifacts.size()).getSubtype());
//            }
//            // 6. 保存PDF
//            pdfDoc.save(processedPdf.getAbsolutePath());
//            return true;
//        } catch (Exception e) {
//            log.error("添加图片水印失败", e);
//            return false;
//        }
//    }

    /**
     * 获取需要处理的页面列表
     */
    private List<Integer> getTargetPages(int totalPages, WatermarkParams params) {
        List<Integer> targetPages = new ArrayList<>();
        // 所有页面
        if ("all".equals(params.getPageRange())) {
            for (int i = 1; i <= totalPages; i++) {
                targetPages.add(i);
            }
        } else {
            // 指定页面
            String[] pageItems = params.getPageList().split(",");
            for (String item : pageItems) {
                if (item.contains("-")) {
                    String[] range = item.split("-");
                    int start = Integer.parseInt(range[0]);
                    int end = Integer.parseInt(range[1]);
                    start = Math.max(1, start);
                    end = Math.min(totalPages, end);
                    for (int i = start; i <= end; i++) {
                        targetPages.add(i);
                    }
                } else {
                    int page = Integer.parseInt(item);
                    if (page >= 1 && page <= totalPages) {
                        targetPages.add(page);
                    }
                }
            }
        }
        return targetPages;
    }

    public boolean addTextWatermark(MultipartFile originalPdf, File processedPdf, WatermarkParams params) {
        try (Document pdfDoc = new Document(originalPdf.getInputStream())) {
            // 获取目标页面列表
            List<Integer> targetPages = getTargetPages(pdfDoc.getPages().size(), params);
            if (targetPages.isEmpty()) {
                log.warn("未匹配到需要添加水印的页面");
                return false;
            }
            PageInfo pageInfo = pdfDoc.getPages().get_Item(1).getPageInfo();
            double pageWidth = pageInfo.getWidth();
            double pageHeight = pageInfo.getHeight();
            double actualX;
            double actualY;
            // 解析通用参数
            String content = params.getContent() == null ? "" : params.getContent();
            int fontSize = (int) (params.getFontSizeRatio() * pageWidth);
            Color color = new Color(Integer.parseInt(params.getColor(), 16));
            double opacity = params.getOpacity() == null ? 0.3 : params.getOpacity();
            int rotate = params.getRotate() == null ? 0 : params.getRotate();
            String fontCode = params.getFontCode() == null ? "SimHei" : params.getFontCode();
            // 坐标适配：PDF原点在左下角，小程序画布只能左上角原点真恶心
            actualX = pageWidth * params.getXRatio() - fontSize * Math.sin(Math.toRadians(rotate));
            actualY = pageHeight - (pageHeight * params.getYRatio() + fontSize * Math.cos(Math.toRadians(rotate)));
            FontRepository.loadFonts();
            // 创建 FormattedText
            FormattedText formattedText = new FormattedText(
                    content,
                    color,
                    fontCode,
                    EncodingType.Identity_h,
                    // 是否加粗（可扩展为参数）
                    true,
                    // 字体大小
                    fontSize
            );
            // 遍历目标页面添加水印
            for (int pageNum : targetPages) {
                // 创建 WatermarkArtifact（标准水印类型）
                WatermarkArtifact watermark = new WatermarkArtifact();
                // 标记为原生水印
                watermark.setSubtype(Artifact.ArtifactSubtype.Watermark);
                // 绑定格式化文字
                watermark.setText(formattedText);
                // 设置透明度
                watermark.setOpacity(opacity);
                // 设置旋转角度
                watermark.setRotation(360 - rotate);
                com.aspose.pdf.Point position = new com.aspose.pdf.Point(actualX, actualY);
                watermark.setPosition(position);


                // 添加到页面 Artifacts（核心：替代 Stamp，标记为标准水印）
                pdfDoc.getPages().get_Item(pageNum).getArtifacts().add(watermark);
            }

            // 保存PDF
            pdfDoc.save(processedPdf.getAbsolutePath());
            log.info("文字水印添加完成，输出路径：{}", processedPdf.getAbsolutePath());
            return true;

        } catch (Exception e) {
            log.error("添加文字水印失败", e);
            return false;
        }
    }

    public boolean addImageWatermark(MultipartFile originalPdf, File processedPdf, WatermarkParams params) {
        InputStream imageInputStream = null;
        File tempFile = null;
        try (com.aspose.pdf.Document pdfDoc = new com.aspose.pdf.Document(originalPdf.getInputStream())) {
            BufferedImage originalImage = base64ToImage(params.getImageBase64());
            PageInfo pageInfo = pdfDoc.getPages().get_Item(1).getPageInfo();
            double pageWidth = pageInfo.getWidth();
            double pageHeight = pageInfo.getHeight();
            double pdfImgWidth = params.getWidthRatio() * pageWidth;
            double pdfImgHeight = params.getHeightRatio() * pageHeight;
            double actualX = 0;
            double actualY = 0;
            log.info("pageWidth: {}, pageHeight: {}, pdfImgWidth: {}, pdfImgHeight: {}", pageWidth, pageHeight, pdfImgWidth, pdfImgHeight);
            BufferedImage scaledImage = scaleImage(originalImage, pdfImgWidth, pdfImgHeight);
            tempFile = File.createTempFile("watermark_", ".png");
            ImageIO.write(scaledImage, "png", new FileOutputStream(tempFile));
            List<Integer> targetPages = getTargetPages(pdfDoc.getPages().size(), params);

            actualX = pageWidth * params.getXRatio() - pdfImgHeight * Math.sin(Math.toRadians(params.getRotate()));
            actualY = pageHeight - (pageHeight * params.getYRatio() + pdfImgHeight * Math.cos(Math.toRadians(params.getRotate())));

            for (int pageNum : targetPages) {
                WatermarkArtifact watermark = new WatermarkArtifact();
                imageInputStream = new FileInputStream(tempFile);
                watermark.setImage(imageInputStream);
                watermark.setOpacity(params.getOpacity());
                watermark.setRotation(360 - params.getRotate());
                watermark.setSubtype(Artifact.ArtifactSubtype.Watermark);
                com.aspose.pdf.Point position = new com.aspose.pdf.Point(actualX, actualY);
                watermark.setPosition(position);

                pdfDoc.getPages().get_Item(pageNum).getArtifacts().add(watermark);
            }
            pdfDoc.save(processedPdf.getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("添加图片水印失败", e);
            return false;
        } finally {
            if (imageInputStream != null) {
                try {
                    imageInputStream.close();
                } catch (IOException e) {
                    log.error("关闭图片输入流失败", e);
                }
            }
            if (tempFile != null) {
                tempFile.deleteOnExit();
            }
        }
    }

    /**
     * Base64转BufferedImage
     */
    public BufferedImage base64ToImage(String base64Str) {
        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Str);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            return ImageIO.read(bis);
        } catch (Exception e) {
            throw new RuntimeException("图片解码失败", e);
        }
    }

    public static BufferedImage scaleImage(BufferedImage original, double targetWidth, double targetHeight) {
        int width = Math.round((float) targetWidth);
        int height = Math.round((float) targetHeight);
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        // 开启抗锯齿，保证缩放质量
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, width, height, null);
        g2d.dispose();
        return scaledImage;
    }

    public static void main(String[] args) throws IOException {
//        BufferedImage originalImage = new BufferedImage(100,100,BufferedImage.TYPE_INT_ARGB);
        FileOutputStream os = new FileOutputStream("C:\\Users\\Wu\\Desktop\\demo\\pdf\\转换后.pdf");
        File imageFile = new File("C:\\Users\\Wu\\Desktop\\demo\\pdf\\baota.jpg");
        BufferedImage image = ImageIO.read(imageFile);
//        InputStream inputStream = new FileInputStream("C:\\Users\\Wu\\Desktop\\demo\\pdf\\baota.jpg");
        InputStream docin = new FileInputStream("C:\\Users\\Wu\\Desktop\\demo\\pdf\\demooooo.pdf");

        BufferedImage scaledImage = scaleImage(image, 100, 100);
        File tempFile = File.createTempFile("watermark_", ".png");
        ImageIO.write(scaledImage, "png", new FileOutputStream(tempFile));
        InputStream inputStream = new FileInputStream(tempFile);
        com.aspose.pdf.Document doc = new com.aspose.pdf.Document(docin);//加载源文件数据
        WatermarkArtifact watermarkArtifact = new WatermarkArtifact();
        watermarkArtifact.setImage(inputStream);
        watermarkArtifact.setOpacity(0.5);
//        watermarkArtifact.setRotation(360 - 45);
        watermarkArtifact.setSubtype(Artifact.ArtifactSubtype.Watermark);
        com.aspose.pdf.Point position = new com.aspose.pdf.Point(0, 0);
        watermarkArtifact.setPosition(position);
        doc.getPages().get_Item(1).getArtifacts().add(watermarkArtifact);
        doc.save(os);
        tempFile.deleteOnExit();
    }
}