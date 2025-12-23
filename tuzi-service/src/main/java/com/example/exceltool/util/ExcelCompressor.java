package com.example.exceltool.util;


import com.aspose.cells.License;
import com.aspose.cells.OoxmlCompressionType;
import com.aspose.cells.Workbook;
import com.aspose.cells.XlsbSaveOptions;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Log4j2
public class ExcelCompressor {
    static {
        InputStream is = ExcelConverter.class.getClassLoader().getResourceAsStream("license.xml");
        License license = new License();
        license.setLicense(is);
        log.info("Excel 24.12 许可证已注册");
    }

    /**
     * 压缩 Word 文档（支持 24.12）
     */
    public static void compressExcel(MultipartFile file, String outputPath, int level) throws Exception {
        Workbook workbook = new Workbook(file.getInputStream());
        XlsbSaveOptions options = new XlsbSaveOptions();
        long l = System.currentTimeMillis();
        log.info("开始压缩文件: {} 到 {} 格式", file.getOriginalFilename(), outputPath);
        int compressionLevel = getCompressionLevel(level);
        options.setCompressionType(compressionLevel);
        workbook.save(outputPath, options);
        log.info("压缩文件: {} 到 {} 格式耗时: {} 毫秒", file.getOriginalFilename(), outputPath, System.currentTimeMillis() - l);
    }

    private static int getCompressionLevel(int level) {
        switch (level) {
            case 1:
                return OoxmlCompressionType.LEVEL_1;
            case 5:
                return OoxmlCompressionType.LEVEL_5;
            case 9:
                return OoxmlCompressionType.LEVEL_9;
            default:
                return OoxmlCompressionType.LEVEL_1;
        }
    }

//    public static void main(String[] args) throws Exception {
//        Workbook workbook = new Workbook("C:\\Users\\Wu\\Desktop\\工单统计1127.xlsx");
//        XlsbSaveOptions options = new XlsbSaveOptions();
//        options.setCompressionType(OoxmlCompressionType.LEVEL_1);
//        long startTime = System.nanoTime();
//        workbook.save("C:\\Users\\Wu\\Desktop\\LargeSampleFile_level_1_out.xlsb", options);
//        long endTime = System.nanoTime();
//        long timeElapsed = endTime - startTime;
//        System.out.println("Level 1 Elapsed Time: " + timeElapsed / 1000000);
//
//        startTime = System.nanoTime();
//        options.setCompressionType(OoxmlCompressionType.LEVEL_6);
//        workbook.save("C:\\Users\\Wu\\Desktop\\LargeSampleFile_level_6_out.xlsb", options);
//        endTime = System.nanoTime();
//        timeElapsed = endTime - startTime;
//        System.out.println("Level 6 Elapsed Time: " + timeElapsed / 1000000);
//
//        startTime = System.nanoTime();
//        options.setCompressionType(OoxmlCompressionType.LEVEL_9);
//        workbook.save("C:\\Users\\Wu\\Desktop\\LargeSampleFile_level_9_out.xlsb", options);
//        endTime = System.nanoTime();
//        timeElapsed = endTime - startTime;
//        System.out.println("Level 9 Elapsed Time: " + timeElapsed / 1000000);
//    }
}