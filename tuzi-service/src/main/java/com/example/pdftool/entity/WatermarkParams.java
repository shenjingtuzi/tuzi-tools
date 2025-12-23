package com.example.pdftool.entity;

import lombok.Data;

@Data
public class WatermarkParams {
    private String type; // text/image
    private String imageBase64; // 图片Base64编码
    // 文字水印参数
    private String content;
    private double fontSizeRatio;
    private String fontCode;
    private String color;
    private Double opacity;
    private Integer rotate;
    private double  xRatio;
    private double  yRatio;
    // 图片水印参数
    private String imageUrl;
    private Double widthRatio;
    private Double heightRatio;
    // 页面范围
    private String pageRange;
    private String pageList;
}
