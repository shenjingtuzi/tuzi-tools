package com.example.pdftool.entity;

import lombok.Data;

@Data
public class WatermarkInfo {

    /**
     * 水印ID
     */
    private Integer id;

    /**
     * 水印类型
     */
    private String type;

    /**
     * 水印内容
     */
    private String content;

}
