package com.seehold.query;

import lombok.Data;
import org.springframework.ai.tool.annotation.ToolParam;

@Data
public class EmbedQuery {

    @ToolParam(description = "当前用户Id：需要把用户id传入")
    private String userId;

    @ToolParam(description = "用户的prompt")
    private String query;

    @ToolParam(description = "返回相似度最近的条目数量，默认3，最大10")
    private int topK = 3;

    @ToolParam(description = "知识分类，如['Java','Mysql','Spring']")
    private String category;

}
