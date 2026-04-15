package com.seehold.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MetaData {

    //统一用String存userId
    private String userId;

    private String source;

    private String category;

    private int lineNumber;

    private int totalLines;

    private String uploadTime;

}
