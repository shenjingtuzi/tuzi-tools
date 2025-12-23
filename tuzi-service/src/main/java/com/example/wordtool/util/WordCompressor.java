package com.example.wordtool.util;

import com.aspose.words.CompressionLevel;
import com.aspose.words.Document;
import com.aspose.words.NodeCollection;
import com.aspose.words.NodeType;
import com.aspose.words.OoxmlSaveOptions;
import com.aspose.words.Shape;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static com.example.wordtool.util.WordConverter.registerWord2412;

@Log4j2
public class WordCompressor {

    /**
     * 压缩 Word 文档（支持 24.12）
     */
    public static void compressWord(MultipartFile file, String outputPath, int level) throws Exception {
        registerWord2412();
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

    private static byte[] compressPicture(BufferedImage image, int level) {
        // 由开发人员选择用于图像压缩的库。
        // 例如，使用 javax.imageio.ImageIO 压缩图像。
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        String outputFormat;
        float  outputQuality;
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
}