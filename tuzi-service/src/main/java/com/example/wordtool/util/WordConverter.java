package com.example.wordtool.util;

import com.aspose.words.Document;
import com.aspose.words.ImageSaveOptions;
import com.aspose.words.PageSet;
import com.aspose.words.SaveFormat;
import lombok.extern.log4j.Log4j2;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Log4j2
public class WordConverter {

    /**
     * 将 Word 文档转换为 PDF 文档
     * @param docFile 输入的 Word 文件路径 (.doc 或 .docx)
     * @param targetFile 输出的目标文件路径
     * @param targetType 目标文件类型
     * @throws IOException 当文件读取或写入失败时抛出
     */
    public static void convert(File docFile, File targetFile, String targetType) throws Exception {
        registerWord2412();
        if (!docFile.exists()) {
            throw new FileNotFoundException("输入文件不存在: " + docFile.getPath());
        }
        long l = System.currentTimeMillis();
        log.info("开始转换文件: {} 到 {} 格式", docFile.getPath(), targetType);
        if ("png".equalsIgnoreCase(targetType)) {
            // 4. 生成 ZIP 压缩包（流式处理，无需临时文件）
            try (FileOutputStream fos = new FileOutputStream(targetFile);
                 ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
                Document doc = new Document(docFile.getAbsolutePath());
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
            Document doc = new Document(docFile.getAbsolutePath());
            if ("pdf".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.PDF);
            } else if ("png".equalsIgnoreCase(targetType)) {

            } else if ("xlsx".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.XLSX);
            } else if ("md".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.MARKDOWN);
            } else if ("html".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.HTML);
            }  else if ("rtf".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.RTF);
            }  else if ("xps".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.XPS);
            }  else {
                throw new IllegalArgumentException("不支持的目标文件类型: " + targetType);
            }
        } catch (Exception e) {
            throw new IOException("转换失败: " + e.getMessage(), e);
        } finally {
            log.info("转换文件: {} 到 {} 格式耗时: {} 毫秒", docFile.getPath(), targetType, System.currentTimeMillis() - l);
        }
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