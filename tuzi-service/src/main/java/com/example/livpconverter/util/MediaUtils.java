package com.example.livpconverter.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Slf4j
@Component
public class MediaUtils {

    @Value("${ffmpeg.path}")
    private String ffmpegPath;

    @Value("${heif-enc.path}")
    private String heifEncPath;

    @Value("${exiftool.path}")
    private String exiftoolPath;

    /**
     * 执行命令行命令，并返回输出日志
     */
    private void executeCommand(String[] command) throws IOException, InterruptedException {
        log.info("执行命令: {}", String.join(" ", command));

        Process process = new ProcessBuilder(command)
                .redirectErrorStream(true) // 合并标准输出和错误输出
                .start();

        // 读取输出，防止进程阻塞
        try (InputStream inputStream = process.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.info("命令输出: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("命令执行失败, 退出码: " + exitCode + ". 命令: " + String.join(" ", command));
        }
    }

    /**
     * 使用 heif-enc 将图片转换为 HEIC
     */
    public void convertToHeic(String inputImagePath, String outputHeicPath) throws IOException, InterruptedException {
        String[] command = {
                heifEncPath,
                inputImagePath,
                "-o", outputHeicPath
        };
        executeCommand(command);
        log.info("HEIC 文件生成成功: {}", outputHeicPath);
    }

    /**
     * 使用 ffmpeg 将视频转换为 MOV，并添加 Live Photo 元数据
     * @param inputVideoPath 输入视频路径
     * @param outputMovPath 输出 MOV 路径
     * @param uuid 用于 ContentIdentifier 的唯一ID
     */
    public void convertToMovWithLivePhotoMeta(String inputVideoPath, String outputMovPath, String uuid) throws IOException, InterruptedException {
        // 注意：-ss 和 -t 的位置很重要，放在 -i 之前可以实现快速seek，但可能不精确。放在之后则是精确裁剪，但速度慢。
        // 为了兼容性，这里放在 -i 之后。
        String[] command = {
                ffmpegPath,
                "-i", inputVideoPath,
                "-ss", "00:00:00.5",   // 从视频的 0.5 秒处开始截取
                "-t", "00:00:02.5",    // 截取 2.5 秒长的片段
                "-c", "copy",          // 流复制，不重新编码
                "-an",                 // 移除音频
                "-metadata", "com.apple.quicktime.content.identifier=" + uuid,
                "-metadata", "com.apple.quicktime.live-photo=1",
                "-y",                  // 覆盖输出文件
                outputMovPath
        };
        executeCommand(command);
        log.info("MOV 文件（带 Live Photo 元数据）生成成功: {}", outputMovPath);
    }

    /**
     * 使用 exiftool 为 HEIC 文件添加 Live Photo 所需的元数据
     * @param heicPath HEIC 文件路径
     * @param uuid 用于 MakerNote 和 ContentIdentifier 的唯一ID
     */
    public void addLivePhotoMetaToHeic(String heicPath, String uuid) throws IOException, InterruptedException {
        // -overwrite_original 参数会直接修改原文件，而不创建备份
        String[] command = {
                exiftoolPath,
                "-MakerNote=" + uuid,
                "-ContentIdentifier=" + uuid,
                "-overwrite_original",
                heicPath
        };
        executeCommand(command);
        log.info("HEIC 文件元数据添加成功: {}", heicPath);
    }
}