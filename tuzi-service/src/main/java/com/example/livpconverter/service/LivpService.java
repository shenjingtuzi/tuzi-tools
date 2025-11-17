package com.example.livpconverter.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.File;

public interface LivpService {
    /**
     * 转换视频为LIVP文件
     * @param videoFile 视频文件
     * @param coverImageFile 封面图片文件
     * @return 生成的LIVP文件
     * @throws Exception 转换过程中的异常
     */
    File convertToLivp(MultipartFile videoFile, MultipartFile coverImageFile) throws Exception;
}