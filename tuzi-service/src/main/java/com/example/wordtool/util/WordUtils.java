package com.example.wordtool.util;

import com.aspose.words.CompressionLevel;
import com.aspose.words.Document;
import com.aspose.words.FileFormatInfo;
import com.aspose.words.FileFormatUtil;
import com.aspose.words.ImageSaveOptions;
import com.aspose.words.NodeCollection;
import com.aspose.words.NodeType;
import com.aspose.words.OoxmlSaveOptions;
import com.aspose.words.PageSet;
import com.aspose.words.ProtectionType;
import com.aspose.words.SaveFormat;
import com.aspose.words.Shape;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
public class WordUtils {

    static {
        registerWord2412();
    }

    /**
     * 将 Word 文档转换为 PDF 文档
     *
     * @param file       输入的 Word 文件路径 (.doc 或 .docx)
     * @param targetFile 输出的目标文件路径
     * @param targetType 目标文件类型
     * @throws IOException 当文件读取或写入失败时抛出
     */
    public static void convert(MultipartFile file, File targetFile, String targetType) throws Exception {
        Document doc = new Document(file.getInputStream());
        long l = System.currentTimeMillis();
        log.info("开始转换文件: {} 到 {} 格式", file.getOriginalFilename(), targetType);
        if ("png".equalsIgnoreCase(targetType)) {
            // 4. 生成 ZIP 压缩包（流式处理，无需临时文件）
            try (FileOutputStream fos = new FileOutputStream(targetFile);
                 ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
                // 3. 配置 PNG 输出选项
                ImageSaveOptions imageOptions = new ImageSaveOptions(SaveFormat.PNG);
                imageOptions.setUseAntiAliasing(true); // 启用抗锯齿（让文字/线条更平滑）
                imageOptions.setUseHighQualityRendering(true); // 高质量渲染

                // 5. 遍历 Word 所有页面，逐个转换为 PNG 并写入 ZIP
                for (int pageIndex = 0; pageIndex < doc.getPageCount(); pageIndex++) {

                    // 设置当前转换的页面（Aspose.Words 页面索引从 0 开始）
                    imageOptions.setPageSet(new PageSet(pageIndex));

                    // 定义 ZIP 中的文件名（格式：页面1.png、页面2.png... 避免乱序）
                    String pngFileName = String.format("页面%d.png", pageIndex + 1);
                    ZipEntry zipEntry = new ZipEntry(pngFileName);
                    zos.putNextEntry(zipEntry); // 新建 ZIP 条目

                    // 6. Word 页面转 PNG，直接写入 ZIP（无临时文件，节省磁盘空间）
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                        doc.save(baos, imageOptions); // 页面转 PNG 到字节流
                        baos.writeTo(zos); // 字节流写入 ZIP
                    }

                    zos.closeEntry(); // 关闭当前 ZIP 条目
                }

            } catch (Exception e) {
                // 若失败，删除不完整的 ZIP 文件
//                new File(outputZipPath).delete();
                throw new Exception("Word 转 PNG 打包 ZIP 失败：" + e.getMessage(), e);
            }
        }

        try {
            // 加载Word文档
            if ("pdf".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.PDF);
            } else if ("png".equalsIgnoreCase(targetType)) {

            } else if ("xlsx".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.XLSX);
            } else if ("md".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.MARKDOWN);
            } else if ("html".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.HTML);
            } else if ("rtf".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.RTF);
            } else if ("xps".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.XPS);
            } else {
                throw new IllegalArgumentException("不支持的目标文件类型: " + targetType);
            }
        } catch (Exception e) {
            throw new IOException("转换失败: " + e.getMessage(), e);
        } finally {
            log.info("转换文件: {} 到 {} 格式耗时: {} 毫秒", file.getOriginalFilename(), targetType, System.currentTimeMillis() - l);
        }
    }

    /**
     * 压缩 Word 文档（支持 24.12）
     */
    public static void compressWord(MultipartFile file, String outputPath, int level) throws Exception {
        Document doc = new Document(file.getInputStream());
        doc.cleanup();
        long l = System.currentTimeMillis();
        log.info("开始压缩文件: {} 到 {} 格式", file.getOriginalFilename(), outputPath);
        NodeCollection nodes = doc.getChildNodes(NodeType.SHAPE, true);
        for (Shape shape : (Iterable<Shape>) nodes) {
            if (shape.isImage()) {
                // 由开发人员选择用于图像压缩的库。
                BufferedImage image = ImageIO.read(shape.getImageData().toStream());
                byte[] compressedImage = compressPicture(image, level);
                // 压缩图像并将其设置回形状。
                shape.getImageData().setImage(new ByteArrayInputStream(compressedImage));
            }
        }


        OoxmlSaveOptions saveOptions = new OoxmlSaveOptions();
        // CompressionLevel.MAXIMUM 压缩级别
        saveOptions.setCompressionLevel(getCompressionLevel(level));

        doc.save(outputPath, saveOptions);
        log.info("压缩文件: {} 到 {} 格式耗时: {} 毫秒", file.getOriginalFilename(), outputPath, System.currentTimeMillis() - l);
    }


    public static void encryptWord(File file, String path, String password) {
        try {
            Document doc = new Document(file.getAbsolutePath());
            doc.protect(ProtectionType.READ_ONLY, password);
            doc.save(path);
        } catch (Exception e) {
            log.info("word 加密失败: {}", e.getMessage());
        }
    }

    public static void decryptWord(File file, String path, String password) {
        try {
            Document doc = new Document(file.getAbsolutePath());
            doc.unprotect(password);
            doc.save(path);
        } catch (Exception e) {
            log.info("word 解密失败: {}", e.getMessage());
        }
    }

    public static boolean isEncrypted(File file) {
        try {
            FileFormatInfo fileFormatInfo = FileFormatUtil.detectFileFormat(file.getAbsolutePath());
            return fileFormatInfo.isEncrypted();
        } catch (Exception e) {
            log.info("检查word文档是否加密失败: {}", e.getMessage());
            return false;
        }
    }


    private static byte[] compressPicture(BufferedImage image, int level) {
        // 由开发人员选择用于图像压缩的库。
        // 例如，使用 javax.imageio.ImageIO 压缩图像。
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String outputFormat;
        float outputQuality;
        switch (level) {
            case 1 -> {
                outputFormat = "PNG";
                outputQuality = 0.1f;
            }
            case 2 -> {
                outputFormat = "JPG";
                outputQuality = 0.2f;
            }
            case 3 -> {
                outputFormat = "JPG";
                outputQuality = 0.05f;
            }
            default -> {
                outputFormat = "PNG";
                outputQuality = 0.1f;
            }
        }
        try {
            Thumbnails.of(image)
                    .scale(1.0)
                    .outputQuality(outputQuality)
                    .outputFormat(outputFormat)
                    .toOutputStream(baos);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }


    private static int getCompressionLevel(int level) {
        return switch (level) {
            case 1 -> CompressionLevel.FAST;
            case 2 -> CompressionLevel.NORMAL;
            case 3 -> CompressionLevel.MAXIMUM;
            default -> CompressionLevel.NORMAL;
        };
    }

    public static void registerWord2412() {
        try {
            Class<?> zzodClass = Class.forName("com.aspose.words.zzod");
            Constructor<?> constructors = zzodClass.getDeclaredConstructors()[0];
            constructors.setAccessible(true);
            Object instance = constructors.newInstance(null, null);
            Field zzWws = zzodClass.getDeclaredField("zzWws");
            zzWws.setAccessible(true);
            zzWws.set(instance, 1);
            Field zzVZC = zzodClass.getDeclaredField("zzVZC");
            zzVZC.setAccessible(true);
            zzVZC.set(instance, 1);

            Class<?> zz83Class = Class.forName("com.aspose.words.zz83");
            constructors.setAccessible(true);
            constructors.newInstance(null, null);

            Field zzZY4 = zz83Class.getDeclaredField("zzZY4");
            zzZY4.setAccessible(true);
            ArrayList<Object> zzwPValue = new ArrayList<>();
            zzwPValue.add(instance);
            zzZY4.set(null, zzwPValue);

            Class<?> zzXuRClass = Class.forName("com.aspose.words.zzXuR");
            Field zzWE8 = zzXuRClass.getDeclaredField("zzWE8");
            zzWE8.setAccessible(true);
            zzWE8.set(null, 128);
            Field zzZKj = zzXuRClass.getDeclaredField("zzZKj");
            zzZKj.setAccessible(true);
            zzZKj.set(null, false);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}