package com.example.pdftool.util;

import com.aspose.pdf.Artifact;
import com.aspose.pdf.ArtifactCollection;
import com.aspose.pdf.Document;
import com.aspose.pdf.FontRepository;
import com.aspose.pdf.Page;
import com.aspose.pdf.PageInfo;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.TextFragmentAbsorber;
import com.aspose.pdf.TextReplaceOptions;
import com.aspose.pdf.WatermarkArtifact;
import com.aspose.pdf.XImage;
import com.aspose.pdf.facades.EncodingType;
import com.aspose.pdf.facades.FormattedText;
import com.example.pdftool.entity.WatermarkInfo;
import com.example.pdftool.entity.WatermarkParams;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Log4j2
@Component
public class PDFWatermarkUtils {

    private static final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    /**
     * 获取需要处理的页面列表
     */
    private static List<Integer> getTargetPages(int totalPages, WatermarkParams params) {
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

    public static boolean addTextWatermark(MultipartFile originalPdf, File processedPdf, WatermarkParams params) {
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

    public static boolean addImageWatermark(MultipartFile originalPdf, File processedPdf, WatermarkParams params) {
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

    public static List<WatermarkInfo> getWatermark(String inputPath) {
        Integer id = 1;
        List<WatermarkInfo> watermarkList = new ArrayList<>();
        FileOutputStream os = null;
        Document pdf = null;
        try {
            pdf = new Document(inputPath);
//            for (Page page : pdf.getPages()) {
            ArtifactCollection artifacts = pdf.getPages().get_Item(1).getArtifacts();
            for (Artifact artifact : artifacts) {
                if (artifact.getSubtype() == Artifact.ArtifactSubtype.Watermark) {
                    WatermarkInfo watermarkInfo = new WatermarkInfo();
                    watermarkInfo.setId(id);
                    String text = artifact.getText();
                    if (StringUtils.isNotBlank(text)) {
                        watermarkInfo.setType("text");
                        watermarkInfo.setContent(text);
                    } else {
                        watermarkInfo.setType("image");
                        String watermarkImageName = UUID.randomUUID() + ".png";
                        File watermarkImageFile = uploadPath.resolve(watermarkImageName).toFile();
                        String pngUrl = "/api/file/output/" + watermarkImageName;
                        XImage image = artifact.getImage();
                        os = new FileOutputStream(watermarkImageFile);
                        image.save(os, 1);
                        watermarkInfo.setContent(pngUrl);
                        watermarkImageFile.delete();
                    }
                    watermarkList.add(watermarkInfo);
                    id++;
                    log.info("水印ID: {}, 水印类型: {}", id, artifact.getSubtype());
                }
            }
//            }
        } catch (Exception e) {
            throw new RuntimeException("获取水印失败：" + e.getMessage(), e);
        } finally {
            if (pdf != null) {
                pdf.close();
            }
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                    log.error("关闭文件输出流失败：" + e.getMessage(), e);
                }
            }
        }
        return watermarkList;
    }

    /**
     * 移除 PDF 文本水印（修正版）
     *
     * @param inputPath    输入 PDF 路径
     * @param outputPath   输出 PDF 路径
     * @param textToRemove 要删除的水印文字（支持模糊匹配）
     * @param pages        需要处理的页码（null 表示全部，页码从 1 开始）
     */
    public void removeTextWatermark(String inputPath, String outputPath, int waterMarkId,
                                    String textToRemove, List<Integer> pages) {
        try (Document pdf = new Document(inputPath)) {
            for (Page page : pdf.getPages()) {
                int currentPageNum = page.getNumber();
                // 跳过不需要处理的页面
                if (pages != null && !pages.contains(currentPageNum)) {
                    continue;
                }
                ArtifactCollection artifacts = page.getArtifacts();
                if (StringUtils.isNotBlank(textToRemove)) {
                    TextFragmentAbsorber absorber = new TextFragmentAbsorber(textToRemove);
                    absorber.getTextReplaceOptions().setReplaceAdjustmentAction(TextReplaceOptions.ReplaceAdjustment.AdjustSpaceWidth);
                    pdf.getPages().accept(absorber);
                    for (TextFragment fragment : absorber.getTextFragments()) {
                        fragment.setText("");
                    }
                } else {
                    artifacts.delete(artifacts.get_Item(waterMarkId));
                }
            }
            // 6. 保存输出 PDF
            pdf.save(outputPath);
            log.info("文本水印移除完成，输出路径：{}", outputPath);
        } catch (Exception e) {
            throw new RuntimeException("移除文本水印失败：" + e.getMessage(), e);
        }
    }

    /**
     * 移除 PDF 图片水印（修正版）
     * 说明：仅删除符合「水印特征」的图片（如尺寸占页面 80%+、透明度低），避免误删正常图片
     *
     * @param inputPath  输入 PDF 路径
     * @param outputPath 输出 PDF 路径
     * @param pages      需要处理的页码（null 表示全部）
     */
    public void removeImageWatermark(String inputPath, String outputPath, int waterMarkId, List<Integer> pages) {
        try (Document pdf = new Document(inputPath)) {

            for (Page page : pdf.getPages()) {
                int currentPageNum = page.getNumber();
                if (pages != null && !pages.contains(currentPageNum)) {
                    continue;
                }

                ArtifactCollection artifacts = page.getArtifacts();
                artifacts.delete(artifacts.get_Item(waterMarkId));
            }

            pdf.save(outputPath);
            log.info("图片水印移除完成，输出路径：{}", outputPath);
        } catch (Exception e) {
            throw new RuntimeException("移除图片水印失败：" + e.getMessage(), e);
        }
    }

    /**
     * 组合删除：文本水印 + 图片水印
     */
    public static boolean removeAllWatermarks(MultipartFile file, String outputPath, String removeType, String watermarkIds, List<Integer> pages) {
        try (Document pdf = new Document(file.getInputStream())) {
            for (Page page : pdf.getPages()) {
//                int currentPageNum = page.getNumber();
//                if (pages != null && !pages.contains(currentPageNum)) {
//                    continue;
//                }
                if ("all".equals(removeType)) {
                    ArtifactCollection artifacts = page.getArtifacts();
                    for (Artifact artifact : artifacts) {
                        if (artifact.getSubtype() == Artifact.ArtifactSubtype.Watermark) {
                            artifacts.delete(artifact);
                        }
                    }
                } else if ("selected".equals(removeType) && StringUtils.isNotBlank(watermarkIds)) {
                    ArtifactCollection artifacts = page.getArtifacts();
                    List<Integer> ids = Arrays.stream(watermarkIds.split(","))
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    ids.forEach(id -> artifacts.delete(artifacts.get_Item(id)));
                }
            }
            pdf.save(outputPath);
            log.info("所有水印移除完成，输出路径：{}", outputPath);
            return true;
        } catch (Exception e) {
            throw new RuntimeException("移除所有水印失败：" + e.getMessage(), e);
        }
    }

    /**
     * 辅助方法：判断图片是否为水印（核心筛选逻辑，可根据实际场景调整）
     * 判定规则：
     * 1. 图片宽度/高度占页面 80% 以上（水印通常全屏）；
     * 2. 图片位置在页面左上角（水印常见位置）；
     */
    private boolean isWatermarkImage(Page page, XImage img) {
        try {
            // 获取页面尺寸
            double pageWidth = page.getPageInfo().getWidth();
            double pageHeight = page.getPageInfo().getHeight();

            // 获取图片尺寸（转换为页面坐标）
            double imgWidth = img.getWidth();
            double imgHeight = img.getHeight();

            // 判定条件1：图片占页面 80% 以上
            boolean isFullPage = (imgWidth / pageWidth >= 0.8) && (imgHeight / pageHeight >= 0.8);
            // 判定条件2：图片位置在页面左上角（偏移量 < 10）
//            boolean isTopLeft = (img.getPosition().getX() < 10) && (img.getPosition().getY() > pageHeight - 10);
            boolean isTopLeft = true;

            return isFullPage || isTopLeft;
        } catch (Exception e) {
            // 无法判断时默认不删除（避免误删）
            return false;
        }
    }

    /**
     * Base64转BufferedImage
     */
    public static BufferedImage base64ToImage(String base64Str) {
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
}