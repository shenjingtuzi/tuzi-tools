package com.example.livpconverter.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

@Slf4j
@Component
public class FFmpegUtils {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    // 注入 heif-enc 路径
    @Value("${heif-enc.path}")
    private String heifEncPath;

    /**
     * 执行FFmpeg命令
     * @param command 命令数组
     * @throws IOException 执行异常
     * @throws InterruptedException 中断异常
     */
    public void executeCommand(String[] command) throws IOException, InterruptedException {
        log.info("执行FFmpeg命令: {}", String.join(" ", command));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true) // 合并标准输出和错误输出
                .start();

        // 读取输出，防止进程阻塞
        try (InputStream inputStream = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("FFmpeg输出: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg命令执行失败, 退出码: " + exitCode + ". 命令: " + String.join(" ", command));
        }
    }

    /**
     * 将视频片段转换为 MOV 格式 (如果源格式不是)
     * @param inputVideoPath 输入视频路径
     * @param outputMovPath 输出 MOV 路径
     * @throws IOException 执行异常
     * @throws InterruptedException 中断异常
     */
    public void convertToMov(String inputVideoPath, String outputMovPath) throws IOException, InterruptedException {
        String[] command = {
                ffmpegPath,
                "-i", inputVideoPath,
                "-c", "copy", // 直接复制流，快速
                "-an", // 移除音频
                "-y", // 覆盖输出文件
                outputMovPath
        };
        executeCommand(command);
    }

    /**
     * 使用 heif-enc 工具将图片转换为 HEIC
     * @param inputImagePath 输入图片路径 (如 PNG, JPG)
     * @param outputHeicPath 输出 HEIC 路径
     * @throws IOException 执行异常
     * @throws InterruptedException 中断异常
     */
    public void convertToHeic(String inputImagePath, String outputHeicPath) throws IOException, InterruptedException {
        // heif-enc 的基本命令格式: heif-enc [options] input_file -o output_file
        String[] command = {
                heifEncPath,
                inputImagePath,
                "-o", outputHeicPath
        };
        executeCommand(command);
    }
}