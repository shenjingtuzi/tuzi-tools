package com.example.exceltool.util;

import com.aspose.cells.License;
import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@Log4j2
public class ExcelConverter {

    static {
        registerExcel2412();
    }

    /**
     * 将 Excel 文档转换为 目标格式文档
     * @param file 输入的 Word 文件路径 (.doc 或 .docx)
     * @param targetFile 输出的目标文件路径
     * @param targetType 目标文件类型
     * @throws IOException 当文件读取或写入失败时抛出
     */
    public static void convert(MultipartFile file, File targetFile, String targetType) throws IOException {

        long l = System.currentTimeMillis();
        log.info("开始转换文件: {} 到 {} 格式", file.getOriginalFilename(), targetType);
        try {
            Workbook workbook = new Workbook(file.getInputStream());
            if ("pdf".equalsIgnoreCase(targetType)) {
                workbook.save(targetFile.getAbsolutePath(), SaveFormat.PDF);
            } else if ("png".equalsIgnoreCase(targetType)) {
                workbook.save(targetFile.getAbsolutePath(), SaveFormat.PNG);
            } else if ("docx".equalsIgnoreCase(targetType)) {
                workbook.save(targetFile.getAbsolutePath(), SaveFormat.DOCX);
            } else if ("md".equalsIgnoreCase(targetType)) {
                workbook.save(targetFile.getAbsolutePath(), SaveFormat.MARKDOWN);
            } else if ("html".equalsIgnoreCase(targetType)) {
                workbook.save(targetFile.getAbsolutePath(), SaveFormat.HTML);
            }  else if ("csv".equalsIgnoreCase(targetType)) {
                workbook.save(targetFile.getAbsolutePath(), SaveFormat.CSV);
            }  else if ("xps".equalsIgnoreCase(targetType)) {
                workbook.save(targetFile.getAbsolutePath(), SaveFormat.XPS);
            }  else {
                throw new IllegalArgumentException("不支持的目标文件类型: " + targetType);
            }
        } catch (Exception e) {
            throw new IOException("转换失败: " + e.getMessage(), e);
        } finally {
            log.info("转换文件: {} 到 {} 格式耗时: {} 毫秒", file.getOriginalFilename(), targetType, System.currentTimeMillis() - l);
        }
    }

    public static void registerExcel2412() {
        InputStream is = ExcelConverter.class.getClassLoader().getResourceAsStream("license.xml");
        License license = new License();
        license.setLicense(is);
        log.info("Excel 24.12 许可证已注册");
    }

//    public static void main(String[] args) {
//        try {
//            //这一步是完整的jar包路径,选择自己解压的jar目录
//            ClassPool.getDefault().insertClassPath("C:\\Users\\Wu\\Downloads\\aspose-cells-24.12.jar");
//            //获取指定的class文件对象
//            CtClass zzZJJClass = ClassPool.getDefault().getCtClass("com.aspose.cells.u7i");
//            //从class对象中解析获取所有方法
//            CtMethod[] methodA = zzZJJClass.getDeclaredMethods();
//            for (CtMethod ctMethod : methodA) {
//                //获取方法获取参数类型
//                CtClass[] ps = ctMethod.getParameterTypes();
//                //筛选同名方法，入参是Document
//                if (ps.length == 1 && ctMethod.getName().equals("a") && ps[0].getName().equals("org.w3c.dom.Document")) {
//                    System.out.println("ps[0].getName==" + ps[0].getName());
//                    //替换指定方法的方法体
//                    ctMethod.setBody("{a=this;com.aspose.cells.y2d.f(-1);com.aspose.cells.a04.a();return;}");
//                }
//            }
//            //这一步就是将破译完的代码放在桌面上
//            zzZJJClass.writeFile("C:\\Users\\Wu\\Desktop\\");
//
//        } catch (Exception e) {
//            System.out.println("错误==" + e);
//        }
//    }




//    public static void main(String[] args) throws FileNotFoundException {
//        InputStream is = ExcelConverter.class.getClassLoader().getResourceAsStream("license.xml");
//        License license = new License();
//        license.setLicense(is);
//        String sourceFile = "C:\\Users\\Wu\\Desktop\\工单统计1127.xlsx";//输入的文件
//        String targetFile = "C:\\Users\\Wu\\Desktop\\转换后.pdf";//输出的文件
//        try {
//            long old = System.currentTimeMillis();
//            FileOutputStream os = new FileOutputStream(targetFile);
//            Workbook excel = new Workbook(sourceFile);//加载源文件数据
//            excel.save(os, com.aspose.cells.SaveFormat.PDF);//设置转换文件类型并转换
//            os.close();
//            long now = System.currentTimeMillis();
//            System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒");  //转化用时
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

}

