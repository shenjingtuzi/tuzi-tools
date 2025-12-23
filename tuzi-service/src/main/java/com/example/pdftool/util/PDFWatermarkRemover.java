package com.example.pdftool.util;

import com.aspose.pdf.Artifact;
import com.aspose.pdf.ArtifactCollection;
import com.aspose.pdf.Document;
import com.aspose.pdf.ImageType;
import com.aspose.pdf.Page;
import com.aspose.pdf.SaveFormat;
import com.aspose.pdf.TextFragment;
import com.aspose.pdf.TextFragmentAbsorber;
import com.aspose.pdf.TextReplaceOptions;
import com.aspose.pdf.XImage;
import com.aspose.pdf.devices.ImageDevice;
import com.aspose.pdf.devices.PngDevice;
import com.example.pdftool.entity.WatermarkInfo;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


/**
 * @author Wu
 */
@Log4j2
@Component
public class PDFWatermarkRemover {

    private final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    public List<WatermarkInfo> getWatermark(String inputPath) {
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
    public boolean removeAllWatermarks(MultipartFile file, String outputPath, String removeType, String watermarkIds, List<Integer> pages) {
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

    public static void main(String[] args) {
        try {
            long old = System.currentTimeMillis();
//            FileOutputStream os = new FileOutputStream("C:\\Users\\Wu\\Desktop\\demo\\pdf\\转换后.pdf");
            com.aspose.pdf.Document doc = new com.aspose.pdf.Document("C:\\Users\\Wu\\Desktop\\demo\\pdf\\all.pdf");//加载源文件数据
            String watermark = doc.getPages().get_Item(1).getArtifacts().get_Item(1).getText();
            ArtifactCollection artifact = doc.getPages().get_Item(1).getArtifacts();
//            artifact.get_Item(2).setSubtype(Artifact.ArtifactSubtype.Watermark);
//            System.out.println(artifact.get_Item(1).getSubtype());
            for (Artifact artifact1 : artifact) {
                Artifact.ArtifactSubtype subtype = artifact1.getSubtype();
                String text = artifact1.getText();
                if (StringUtils.isBlank(text)) {
                    XImage image = artifact1.getImage();
                    FileOutputStream os = new FileOutputStream("C:\\Users\\Wu\\Desktop\\demo\\pdf\\image.png");
                    image.save(os, ImageType.getPng());
                }
//                System.out.println(text);
                System.out.println(subtype);
            }
            System.out.println(artifact.size());

//            PageCollection pages = doc.getPages();
//            for (Page page : pages) {
//                Artifact item1 = page.getArtifacts().get_Item(1);
//                System.out.println(item1.getSubtype());
//                page.getArtifacts().delete(item1);
//            }

//            doc.getPages().get_Item(2).getArtifacts().delete(artifact);
//            TextState textState = artifact.getTextState();
//            artifact.setTextAndState("shenjing", textState);
//            TextFragmentAbsorber absorber = new TextFragmentAbsorber("月");
//            absorber.getTextReplaceOptions().setReplaceAdjustmentAction(TextReplaceOptions.ReplaceAdjustment.AdjustSpaceWidth);
//            doc.getPages().accept(absorber);
//            for (TextFragment fragment : absorber.getTextFragments()) {
//                fragment.setText("");
//            }

//            doc.save(os);
//            System.out.println(watermark);
//            os.close();


//            TextStamp textStamp = new TextStamp("Sample Stamp");
//// 设置印章定位和样式属性
//            textStamp.setTopMargin(10);
//            textStamp.setHorizontalAlignment(HorizontalAlignment.Center);
//            textStamp.setVerticalAlignment(VerticalAlignment.Top);
//
//// 配置图章的文本外观
//            textStamp.getTextState().setFont(FontRepository.findFont("Arial"));
//            textStamp.getTextState().setFontSize(14.0F);
//            textStamp.getTextState().setFontStyle(com.aspose.pdf.FontStyles.Bold | com.aspose.pdf.FontStyles.Italic);
//            textStamp.getTextState().setForegroundColor(Color.getGreen());
//
//            doc.getPages().forEach(page -> page.addStamp(textStamp));
//            doc.save(os);
//            os.close();


            long now = System.currentTimeMillis();
            System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒");  //转化用时
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 将 Excel 文档转换为 目标格式文档
     * @param pdfFile 输入的 Word 文件路径 (.doc 或 .docx)
     * @param targetFile 输出的目标文件路径
     * @param targetType 目标文件类型
     * @throws IOException 当文件读取或写入失败时抛出
     */
    public void convert(MultipartFile pdfFile, File targetFile, String targetType) throws IOException {

        long l = System.currentTimeMillis();
        log.info("开始转换文件: {} 到 {} 格式", pdfFile.getOriginalFilename(), targetType);
        try (Document doc = new Document(pdfFile.getInputStream())) {
            if ("xlsx".equalsIgnoreCase(targetType)) {
//                ExcelSaveOptions excelsave = new ExcelSaveOptions();
//                excelsave.setMinimizeTheNumberOfWorksheets(true);
                doc.save(targetFile.getAbsolutePath(), SaveFormat.Excel);
            } else if ("pptx".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.Pptx);
            } else if ("docx".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.DocX);
            } else if ("mobi".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.MobiXml);
            } else if ("html".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.Html);
            }  else if ("png".equalsIgnoreCase(targetType)) {
                try (FileOutputStream fos = new FileOutputStream(targetFile);
                     ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
                    for (int pageCount = 1; pageCount <= doc.getPages().size(); pageCount++) {
                        ImageDevice imageDevice = new PngDevice(2480,  3508);
                        // 转换特定页面并将图像保存到流
                        String pngFileName = String.format("页面%d.png", pageCount);
                        ZipEntry zipEntry = new ZipEntry(pngFileName);
                        // 新建 ZIP 条目
                        zos.putNextEntry(zipEntry);
                        // 6. Word 页面转 PNG，直接写入 ZIP（无临时文件，节省磁盘空间）
                        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                            imageDevice.process(doc.getPages().get_Item(pageCount), baos);
                            // 字节流写入 ZIP
                            baos.writeTo(zos);
                        }
                        zos.closeEntry(); // 关闭当前 ZIP 条目
                    }
                }
            } else {
                throw new IllegalArgumentException("不支持的目标文件类型: " + targetType);
            }
        } catch (Exception e) {
            throw new IOException("转换失败: " + e.getMessage(), e);
        } finally {
            log.info("转换文件: {} 到 {} 格式耗时: {} 毫秒", pdfFile.getOriginalFilename(), targetType, System.currentTimeMillis() - l);
        }
    }

}
