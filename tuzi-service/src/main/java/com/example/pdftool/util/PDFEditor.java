package com.example.pdftool.util;

import com.aspose.pdf.Document;
import com.aspose.pdf.Page;
import com.aspose.pdf.Rotation;
import com.aspose.pdf.devices.JpegDevice;
import com.aspose.pdf.devices.Resolution;
import lombok.extern.log4j.Log4j2;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * PDF缩略图生成工具类（适配微信小程序回显，固定最优参数）
 */
@Log4j2
public class PDFEditor {

    private static final Path uploadPath = Paths.get(System.getProperty("user.dir"), "uploads");

    // 缩略图宽高：适配小程序列表/预览展示（宽200px，高280px，比例接近PDF原比例）
    private static final int THUMBNAIL_WIDTH = 400;
    private static final int THUMBNAIL_HEIGHT = 560;
    // 分辨率：150dpi（兼顾清晰度和文件大小，小程序加载不卡顿）
    private static final int THUMBNAIL_RESOLUTION = 150;
    // JPG质量：80（平衡画质和体积，单张图片约50-100KB）
    private static final int THUMBNAIL_QUALITY = 80;

    /**
     * 生成PDF所有页面的缩略图（适配微信小程序回显，固定参数）
     *
     * @param pdfFile 前端传入的PDF文件（MultipartFile）
     * @return 所有页面缩略图的字节数组列表（按页面顺序排列）
     * @throws IOException 流操作/PDF处理异常
     */
    public static List<byte[]> generateAllPageThumbnailsForMiniProgram(File pdfFile) throws IOException {
        // 存储所有页面缩略图字节数组
        List<byte[]> thumbnailList = new ArrayList<>();
        try (Document pdfDocument = new Document(pdfFile.getAbsolutePath())) {
            // 加载PDF文档
            int totalPages = pdfDocument.getPages().size();
            if (totalPages == 0) {
                throw new IOException("该PDF文件没有有效页面");
            }
            // 固定配置缩略图生成器
            Resolution res = new Resolution(THUMBNAIL_RESOLUTION);
            JpegDevice jpegDevice = new JpegDevice(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT, res, THUMBNAIL_QUALITY);
            // 遍历所有页面生成缩略图
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
        }
        return thumbnailList;
    }

    public static boolean generate(File originalPdfFile, File pdfFile, List<String> pageOrder, Map<String, Integer> rotateMap, List<Map<String, Object>> addedPages) {
        try (Document originalDoc = new Document(originalPdfFile.getAbsolutePath());
             Document finalDoc = new Document()) {
            //处理每个页面
            for (String pageId : pageOrder) {
                if (pageId.matches("^page_\\d+$")) {
                    // 原始PDF页面
                    int originalPageIndex = Integer.parseInt(pageId.replace("page_", "")) - 1;
                    Page originalPage = originalDoc.getPages().get_Item(originalPageIndex + 1);
                    Page newPage = finalDoc.getPages().add(originalPage);

                    // 应用旋转
                    if (rotateMap.containsKey(pageId)) {
                        newPage.setRotate(convertToResolution(rotateMap.get(pageId)));
                    }
                } else {
                    // 新增页面（空白页或插入的PDF页面）
                    Map<String, Object> addedPageInfo = findAddedPageById(addedPages, pageId);
                    if (addedPageInfo != null) {
                        String type = (String) addedPageInfo.get("type");

                        if ("blank".equals(type)) {
                            // 处理空白页
                            Page blankPage = finalDoc.getPages().add();
                            blankPage.getPageInfo().setWidth(595); // A4宽度
                            blankPage.getPageInfo().setHeight(842); // A4高度
                        } else if ("inserted_pdf".equals(type)) {
                            // 处理插入的PDF页面
                            Map<String, Object> originalFile = (Map<String, Object>) addedPageInfo.get("originalFile");
                            String fileName = (String) originalFile.get("name");
                            File file = uploadPath.resolve(fileName).toFile();

                            // 加载插入的PDF
                            Document insertedDoc = new Document(file.getAbsolutePath());

                            // 从页面ID中提取出在原始PDF中的页码
                            // 页面ID格式：page_${Date.now()}_${index + 1}
                            int pageNumInOriginal = Integer.parseInt(pageId.split("_")[2]);

                            // 只添加指定的页面
                            Page insertedPage = insertedDoc.getPages().get_Item(pageNumInOriginal);
                            Page newPage = finalDoc.getPages().add(insertedPage);

                            // 应用旋转
                            if (rotateMap.containsKey(pageId)) {
                                newPage.setRotate(convertToResolution(rotateMap.get(pageId)));
                            }
                        }
                    }
                }
            }
            // 5. 保存最终PDF
            finalDoc.save(pdfFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("PDF生成失败）：" + e.getMessage(), e);
            return false;
        }
    }

    private static Map<String, Object> findAddedPageById(List<Map<String, Object>> addedPages, String pageId) {
        for (Map<String, Object> page : addedPages) {
            if (pageId.equals(page.get("id"))) {
                return page;
            }
        }
        return null;
    }

    private static Rotation convertToResolution(int resolution) {
        switch (resolution) {
            case 0:
                return Rotation.None;
            case 90:
                return Rotation.on90;
            case 180:
                return Rotation.on180;
            case 270:
                return Rotation.on270;
            case 360:
                return Rotation.on360;
            default:
                throw new IllegalArgumentException("Invalid resolution value: " + resolution);
        }
    }
}