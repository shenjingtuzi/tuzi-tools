package com.example.wordtool.util;

import com.aspose.words.Document;
import com.aspose.words.SaveFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;

public class WordConverter {

    /**
     * 将 Word 文档转换为 PDF 文档
     * @param docFile 输入的 Word 文件路径 (.doc 或 .docx)
     * @param targetFile 输出的目标文件路径
     * @param targetType 目标文件类型
     * @throws IOException 当文件读取或写入失败时抛出
     */
    public static void convert(File docFile, File targetFile, String targetType) throws IOException {
        registerWord2412();
        if (!docFile.exists()) {
            throw new FileNotFoundException("输入文件不存在: " + docFile.getPath());
        }
        try {
            // 加载Word文档
            Document doc = new Document(docFile.getAbsolutePath());
            if ("pdf".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.PDF);
            } else if ("png".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.PNG);
            } else if ("xlsx".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.XLSX);
            } else if ("jpeg".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.JPEG);
            } else if ("bmp".equalsIgnoreCase(targetType)) {
                doc.save(targetFile.getAbsolutePath(), SaveFormat.BMP);
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