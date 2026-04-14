package com.seehold.query;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmbedResult {

    private String text;

    private double score;

    private MetaData metaData;
}