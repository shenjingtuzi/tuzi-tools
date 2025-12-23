package com.example.pdftool.entity;

import lombok.Data;

import java.util.List;

/**
 * 水印移除VO
 * @author Wu
 */
@Data
public class WatermarkRemoveVo {

    /**
     * 水印ID列表
     */
    private String watermarkIds;

    /**
     * 水印类型
     */
    private String type;

     /**
     * 页面范围
     */
    private List<Integer> targetPages;

}
