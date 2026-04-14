package com.seehold.query;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class EmbedQueryResult {

    private int topK;

    private List<EmbedResult> embeds;

}
