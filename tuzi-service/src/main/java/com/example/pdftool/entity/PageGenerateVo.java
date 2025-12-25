package com.example.pdftool.entity;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PageGenerateVo {

    private List<String> order;

    private Map<String, Integer> rotateMap;

    private List<Map<String, Object>> addedPages;

    private Map<String, Object> originalPdf;
}
