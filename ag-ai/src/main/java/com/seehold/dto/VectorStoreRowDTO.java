package com.seehold.dto;

import lombok.Data;

@Data
public class VectorStoreRowDTO {

    private String id;

    private String content;

    private String userId;

    private String source;

    private String category;

    private String lineNumber;

    private String totalLines;

    private String uploadTime;
}
