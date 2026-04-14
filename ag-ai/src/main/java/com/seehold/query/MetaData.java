package com.seehold.query;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MetaData {

    private String source;

    private String category;

    private int lineNumber;

    private int totalLines;

    private String uploadTime;

}
